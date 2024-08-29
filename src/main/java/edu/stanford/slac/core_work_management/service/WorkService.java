package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.model.workflow.WorkflowState;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.validation.ModelFieldValidationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO.User;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.*;
import static java.util.Collections.emptyList;

@Service
@Log4j2
@Validated
@RequiredArgsConstructor
public class WorkService {
    private final WorkMapper workMapper;
    private final DomainMapper domainMapper;

    private final DomainService domainService;
    private final AuthService authService;

    private final WorkRepository workRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LogService logService;
    private final LocationService locationService;
    private final ShopGroupService shopGroupService;
    private final ModelFieldValidationService modelFieldValidationService;
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
    private final ModelHistoryService modelHistoryService;
    private final ApplicationContext applicationContext;

    /**
     * Create a new work automatically creating the sequence
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    public String createNew(@Valid String domainId, @Valid NewWorkDTO newWorkDTO) {
        // contain the set of all user that will become admin for this new work
        Long newWorkSequenceId = wrapCatch(
                workRepository::getNextWorkId,
                -1
        );
        WorkService self = applicationContext.getBean(WorkService.class);
        return self.createNew(domainId, newWorkSequenceId, newWorkDTO, Optional.of(false));
    }

    /**
     * Create a new work automatically creating the sequence
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    public String createNew(String domainId, @Valid NewWorkDTO newWorkDTO, Optional<Boolean> logIf) {
        // contain the set of all user that will become admin for this new work
        Long newWorkSequenceId = wrapCatch(
                workRepository::getNextWorkId,
                -1
        );
        WorkService self = applicationContext.getBean(WorkService.class);
        return self.createNew(domainId, newWorkSequenceId, newWorkDTO, logIf);
    }

    /**
     * Create a new work
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            maxAttempts = 8,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public String createNew(String domainId, Long workSequence, @Valid NewWorkDTO newWorkDTO, Optional<Boolean> logIf) {
        //check if the domain exists
        assertion(
                DomainNotFound
                        .notFoundById()
                        .errorCode(-1)
                        .id(domainId)
                        .build(),
                () -> domainService.existsById(domainId)
        );
        // fetch WorkType to check if the work type exists and get information
        // about the custom fields
        WorkType workType = wrapCatch(
                () -> workTypeRepository
                        .findById(newWorkDTO.workTypeId())
                        .orElseThrow(
                                () -> WorkTypeNotFound
                                        .notFoundById()
                                        .errorCode(-2)
                                        .workId(newWorkDTO.workTypeId())
                                        .build()
                        ),
                -3
        );

        // check if the parent id exists, in case new work is a sub work
        if(newWorkDTO.parentWorkId() != null) {
            assertion(
                    WorkNotFound
                            .notFoundById()
                            .errorCode(-3)
                            .workId(newWorkDTO.parentWorkId())
                            .build(),
                    () -> wrapCatch(
                            () -> workRepository.existsByDomainIdAndId(domainId, newWorkDTO.parentWorkId()),
                            -4
                    )
            );
        }

        // check the work type against the domain
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("The work type is not part of the domain")
                        .errorDomain("WorkService::createNew")
                        .build(),
                () -> workType.getDomainId().compareTo(domainId) == 0
        );
        // contain the set of all user that will become admin for this new work
        Work workToSave = workMapper.toModel(
                domainId,
                workSequence,
                newWorkDTO
        );

        // validate lov
        modelFieldValidationService.verify(
                workToSave,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );


        // validate location and group shop against the domain
        validateLocationForDomain(workToSave.getDomainId(), workToSave.getLocationId(),  -3);
        validateShopGroupForDomain(workToSave.getDomainId(), workToSave.getShopGroupId(), -4);
        // save work
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -5
        );

        log.info("New Work '{}-{}' has been created by '{}'", savedWork.getWorkNumber(), savedWork.getTitle(), savedWork.getCreatedBy());
        updateWorkAuthorization(savedWork);
        log.info("Update domain statistic");
        domainService.updateDomainStatistics(savedWork.getDomainId());

        // log the creation of the work
        if (logIf.isPresent() && logIf.get()) {
            logService.createNewLogEntry
                    (
                            savedWork.getDomainId(),
                            savedWork.getId(),
                            NewLogEntry.builder()
                                    .title(savedWork.getTitle())
                                    .text(savedWork.getDescription())
                                    .build(),
                            null
                    );
        }
        return savedWork.getId();
    }

    /**
     * Validate the location for the domain
     * check if the location belong to the source domain
     *
     * @param domainId  the domain id
     * @param locationId the id of the location
     * @param errorCode  the error code
     */
    private void validateLocationForDomain(String domainId, String locationId, int errorCode) {
        var location = wrapCatch(() -> locationService.findById(locationId), errorCode);
        assertion(
                InvalidLocation
                        .byLocationNameDomainId()
                        .errorCode(errorCode)
                        .locationName(location.name())
                        .domainId(domainId)
                        .build(),
                () -> (location.domain().id().compareTo(domainId) == 0)
        );
    }

    /**
     * Validate shop group for the domain
     * check if the shop group belong to the source domain
     *
     * @param domainId   the domain id
     * @param shopGroupId the id of the location
     * @param errorCode   the error code
     */
    private void validateShopGroupForDomain(String domainId, String shopGroupId,int errorCode) {
        var shopGroup = wrapCatch(() -> shopGroupService.findByDomainIdAndId(domainId, shopGroupId), errorCode);
        assertion(
                InvalidShopGroup
                        .byShopGroupNameDomainId()
                        .errorCode(errorCode)
                        .shopGroupName(shopGroup.name())
                        .domainId(domainId)
                        .build(),
                () -> (shopGroup.domain().id().compareTo(domainId) == 0)
        );
    }

    /**
     * Update a work
     *
     * @param workId        the id of the work
     * @param updateWorkDTO the DTO to update the work
     */
    @Transactional
    public void update(@NotNull String domainId, @NotNull String workId, @Valid UpdateWorkDTO updateWorkDTO) {
        // fetch stored work to check if the work exists
        Work foundWork = wrapCatch(
                () -> workRepository.findById(workId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                ),
                -2
        );

        // check domain match
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage("The work is not part of the domain")
                        .errorDomain("WorkService::update")
                        .build(),
                () -> foundWork.getDomainId().compareTo(domainId) == 0
        );

        // fetch work type for custom field validation
        WorkType workType = wrapCatch(
                () -> workTypeRepository
                        .findById(foundWork.getWorkTypeId())
                        .orElseThrow(
                                () -> WorkTypeNotFound
                                        .notFoundById()
                                        .errorCode(-3)
                                        .workId(foundWork.getWorkTypeId())
                                        .build()
                        ),
                -4
        );


        // check that all the user in the assignedTo are listed into the shop group
        if (updateWorkDTO.assignedTo() != null) {
            updateWorkDTO.assignedTo().forEach(
                    (user) -> {
                        assertion(
                                () -> shopGroupService.checkContainsAUserEmail(foundWork.getDomainId(), foundWork.getShopGroupId(), user),
                                ControllerLogicException
                                        .builder()
                                        .errorCode(-3)
                                        .errorMessage("The user is not part of the shop group")
                                        .errorDomain("WorkService::update")
                                        .build()
                        );
                    }
            );
        }

        // update the model
        workMapper.updateModel(updateWorkDTO, foundWork);

        // validate lov
        modelFieldValidationService.verify(
                foundWork,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );

        // validate location and group shop against the domain
        validateLocationForDomain(foundWork.getLocationId(), foundWork.getDomainId(), -4);
        validateShopGroupForDomain(foundWork.getShopGroupId(), foundWork.getDomainId(), -5);

        // save the work
        var updatedWork = wrapCatch(
                () -> workRepository.save(foundWork),
                -6
        );
        // update all authorization
        updateWorkAuthorization(updatedWork);

        //update domain statistic
        domainService.updateDomainStatistics(updatedWork.getDomainId());
    }

    /**
     * Update the work authorization
     *
     * @param work the work to update
     */
    private void updateWorkAuthorization(Work work) {
        Set<String> adminUserList = new HashSet<>();
        Set<String> writerUserList = new HashSet<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // delete old authorization
        authService.deleteAuthorizationForResourcePrefix(WORK_AUTHORIZATION_TEMPLATE.formatted(work.getId()));

        // this will fire exception in case the location has not been found
        LocationDTO locationDTO = locationService.findById(work.getLocationId());
        if (authentication != null) {
            // the creator is a writer
            writerUserList.add(work.getCreatedBy());
        }

        // authorize location manager as admin
        adminUserList.add(locationDTO.locationManagerUserId());
        // add shop group as writer in the form of virtual user
        writerUserList.add(SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(work.getShopGroupId()));
        // add assigned to users
        if (work.getAssignedTo() != null) {
            writerUserList.addAll(work.getAssignedTo());
        }

        // some user for various reason could be either admin and read
        // so removing the common from the reader list we are going
        // to give to the user only the higher permission
        // so remove all the admin that are also reader
        writerUserList.removeAll(adminUserList);

        // add admin authorization
        adminUserList.forEach(
                (user) -> {
                    authService.addNewAuthorization(
                            NewAuthorizationDTO.builder()
                                    .authorizationType(Admin)
                                    .owner(user)
                                    .ownerType(User)
                                    .resource(WORK_AUTHORIZATION_TEMPLATE.formatted(work.getId()))
                                    .build()
                    );
                }
        );
        // add user authorization
        writerUserList.forEach(
                (user) -> {
                    authService.addNewAuthorization(
                            NewAuthorizationDTO.builder()
                                    .authorizationType(Write)
                                    .owner(user)
                                    .ownerType(User)
                                    .resource(WORK_AUTHORIZATION_TEMPLATE.formatted(work.getId()))
                                    .build()
                    );
                }
        );
        log.info(
                "Users '{}' has been granted as admin for work {}[{}]",
                String.join(",", adminUserList),
                work.getTitle(),
                work.getId()
        );
    }

    /**
     * Close a work
     *
     * @param domainId      the id of the domain
     * @param workId        the id of the work
     * @param reviewWorkDTO the DTO to close the work
     */
    public void reviewWork(@NotNull String domainId, @NotNull String workId, @Valid ReviewWorkDTO reviewWorkDTO) {
        // check for work existence
        var work = wrapCatch(
                () -> workRepository.findById(workId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                ),
                -1
        );
        // check domain match
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage("The work is not part of the domain")
                        .errorDomain("WorkService::reviewWork")
                        .build(),
                () -> work.getDomainId().compareTo(domainId) == 0
        );
        // check for work status
        assertion(
                () -> work.getCurrentStatus().getStatus() == WorkflowState.ReviewToClose,
                ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage("The work is not closeable")
                        .errorDomain("WorkService::closeWork")
                        .build()
        );
        // update put current status on history
        work.getStatusHistory().addFirst(work.getCurrentStatus());
        // set to close
        work.setCurrentStatus(
                WorkStatusLog.builder()
                        .status(WorkflowState.Closed)
                        .build()
        );
        work.setFollowupDescriptionOnClose(reviewWorkDTO.followUpDescription());
        // save work and unlock
        var savedWork = wrapCatch(
                () -> workRepository.save(work),
                -3
        );
        log.info("Work '{}' has change his status to status '{}' by '{}'", savedWork.getId(), savedWork.getCurrentStatus().getStatus(), savedWork.getCurrentStatus().getChanged_by());
        //update domain statistic
        domainService.updateDomainStatistics(savedWork.getDomainId());
    }

    /**
     * Return the work by his id
     *
     * @param id the id of the work
     * @return the work
     */
    public WorkDTO findWorkById(@NotNull String domainId, @NotNull String id, @Valid WorkDetailsOptionDTO workDetailsOptionDTO) {
        return wrapCatch(
                () -> workRepository.findById(id)
                        .map(w -> workMapper.toDTO(w, workDetailsOptionDTO))
                        .orElseThrow(
                                () -> WorkNotFound
                                        .notFoundById()
                                        .errorCode(-1)
                                        .workId(id)
                                        .build()
                        ),
                -1
        );
    }

    /**
     * Return the work history by his id
     *
     * @param id the id of the work
     * @return the list of work changed during the time
     */
    public List<WorkDTO> findWorkHistoryById(@NotNull String domainId, @NotNull String id) {
        return wrapCatch(
                () -> modelHistoryService.findModelChangesByModelId(Work.class, id)
                        .stream()
                        .map(w -> workMapper.toDTO(w, WorkDetailsOptionDTO.builder().build()))
                        .toList(),
                -1
        );
    }

    /**
     * Return the shop group id by the work id
     *
     * @param workId the id of the work
     * @return the shop group id
     */
    public String getShopGroupIdByWorkId(String workId) {
        return wrapCatch(
                () -> workRepository.findById(workId).map(Work::getShopGroupId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                ),
                -1
        );
    }


    /**
     * Search on all the works
     *
     * @return the list of work
     */
    public List<WorkDTO> searchAllWork(WorkQueryParameterDTO workQueryParameterDTO) {
        var workList = wrapCatch(
                () -> workRepository.searchAll(workMapper.toModel(workQueryParameterDTO)),
                -1
        );
        return workList.stream()
                .map(w -> workMapper.toDTO(w, WorkDetailsOptionDTO.builder().build()))
                .toList();
    }

    /**
     * Search on all the activities
     *
     * @return the found activities
     */
    @Cacheable(
            value = {"work-authorization"},
            key = "{#authentication.principal, #workDTO.shopGroup.id}")
    public List<AuthorizationResourceDTO> getAuthorizationByWork(WorkDTO workDTO, Authentication authentication) {
        if (authentication == null) {
            // if the DTO has been requested by an anonymous user, then the access level is Read
            // in other case will should have been blocked by the security layer
            return emptyList();
        }

        List<AuthorizationResourceDTO> accessList = new ArrayList<>();
        //check if it's a root
        boolean isRoot = authService.checkForRoot(authentication);
        // check if user can write normal field
        accessList.add(AuthorizationResourceDTO.builder()
                .field("*")
                .authorizationType(
                        any(
                                // a root users
                                () -> authService.checkForRoot(authentication),
                                // or a user that has the right as writer on the work
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Write,
                                        WORK_AUTHORIZATION_TEMPLATE.formatted(workDTO.id())
                                ),
                                // user of the shop group are always treated as admin on the work
                                () -> shopGroupService.checkContainsAUserEmail(
                                        // fire not found work exception
                                        workDTO.domain().id(),
                                        workDTO.shopGroup().id(),
                                        authentication.getCredentials().toString()
                                )
                        ) ? AuthorizationTypeDTO.Write : AuthorizationTypeDTO.Read)
                .build());

        // check if can modify location
        accessList.add(AuthorizationResourceDTO.builder()
                .field("location")
                .authorizationType(
                        any(
                                // a root users
                                () -> isRoot,
                                // or a user that has the right as admin on the work
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Admin,
                                        WORK_AUTHORIZATION_TEMPLATE.formatted(workDTO.id())
                                )
                        ) ? AuthorizationTypeDTO.Admin : AuthorizationTypeDTO.Read)
                .build());

        // check if can modify assignTo
        accessList.add(AuthorizationResourceDTO.builder()
                .field("assignTo")
                .authorizationType(
                        any(
                                // a root users
                                () -> isRoot,
                                // or a user that is the leader of the group
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Admin,
                                        SHOP_GROUP_AUTHORIZATION_TEMPLATE.formatted(workDTO.shopGroup().id())
                                )
                        ) ? AuthorizationTypeDTO.Admin : AuthorizationTypeDTO.Read)
                .build());

        return accessList;
    }
}
