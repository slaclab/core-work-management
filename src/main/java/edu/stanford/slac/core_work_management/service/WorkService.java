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
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation;
import edu.stanford.slac.core_work_management.service.workflow.*;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.validation.ModelFieldValidationService;
import jakarta.validation.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    private final ScriptService scriptService;
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
//    @Retryable(
//            maxAttempts = 8,
//            backoff = @Backoff(delay = 100, multiplier = 2)
//    )
    public String createNew(String domainId, Long workSequence, @Valid NewWorkDTO newWorkDTO, Optional<Boolean> logIf) {
        // point, if the work is a sub work, to the parent work
        Work parentWork;

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

        // check if the new work that is being created is valid for the workflow
        isValidForWorkflow(domainId, NewWorkValidation.builder().newWorkDTO(newWorkDTO).workType(workType).build());

        // check if the parent id exists, in case new work is a sub work
        if (newWorkDTO.parentWorkId() != null) {
            // now check if the parent permit to have children
            parentWork = checkParentWorkflowForChild(domainId, newWorkDTO.parentWorkId());
        } else {
            parentWork = null;
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
        if (workToSave.getLocationId() != null) {
            validateLocationForDomain(workToSave.getDomainId(), workToSave.getLocationId(), -3);
        }
        if (workToSave.getShopGroupId() != null) {
            validateShopGroupForDomain(workToSave.getDomainId(), workToSave.getShopGroupId(), -4);
        }


        // save work
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -5
        );

        log.info("New Work '{}-{}' has been created by '{}'", savedWork.getWorkNumber(), savedWork.getTitle(), savedWork.getCreatedBy());
        updateWorkAuthorization(savedWork);

        // after this work is update we need to update all the
        // tree up to the ancestor
        if (parentWork != null) {
            workTypeRepository.findByDomainIdAndId(domainId, parentWork.getWorkTypeId())
                    .ifPresentOrElse(
                            wt -> updateParentWorkWorkflow(domainId, parentWork, wt),
                            () -> {
                                throw WorkTypeNotFound.notFoundById()
                                        .workId(parentWork.getWorkTypeId())
                                        .build();
                            }
                    );
//            updateParentWorkWorkflow(domainId, parentWork);
        }

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
     * Update a work
     *
     * @param workId        the id of the work
     * @param updateWorkDTO the DTO to update the work
     */
    @Transactional
    public void update(@NotNull String domainId, @NotNull String workId, @Valid UpdateWorkDTO updateWorkDTO) {
        DomainDTO domain = domainService.findById(domainId);
        // fetch stored work to check if the work exists
        Work foundWork = wrapCatch(
                () -> workRepository.findByDomainIdAndId(domainId, workId).orElseThrow(
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


        // check if the new work that is being created is valid for the workflow
        isValidForWorkflow(
                UpdateWorkValidation
                        .builder()
                        .updateWorkDTO(updateWorkDTO)
                        .workType(workType)
                        .existingWork(foundWork)
                        .build()
        );

        // update the model
        workMapper.updateModel(updateWorkDTO, foundWork);

        // validate lov
        modelFieldValidationService.verify(
                foundWork,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );

        // validate location and group shop against the domain
        if (foundWork.getLocationId() != null) {
            validateLocationForDomain(foundWork.getDomainId(), foundWork.getLocationId(), -4);
        }
        if (foundWork.getShopGroupId() != null) {
            validateShopGroupForDomain(foundWork.getDomainId(), foundWork.getShopGroupId(), -5);
        }

        // lastly we need to update the workflow
        updateWorkWorkflow(domainId, foundWork, workType, domainMapper.toModel(updateWorkDTO.workflowStateUpdate()));

        // save the work
        var updatedWork = wrapCatch(
                () -> workRepository.save(foundWork),
                -6
        );

        // after this work is update we need to update all the
        // tree up to the ancestor
        if (foundWork.getParentWorkId() != null) {
            // find parent work
            var parentWork = wrapCatch(
                    () -> workRepository.findByDomainIdAndId(domainId, foundWork.getParentWorkId()).orElseThrow(
                            () -> WorkNotFound
                                    .notFoundById()
                                    .errorCode(-1)
                                    .workId(foundWork.getParentWorkId())
                                    .build()
                    ),
                    -7
            );
            workTypeRepository.findByDomainIdAndId(domainId, parentWork.getWorkTypeId())
                    .ifPresentOrElse(
                            wt -> updateParentWorkWorkflow(domainId, parentWork, wt),
                            () -> {
                                throw WorkTypeNotFound.notFoundById()
                                        .workId(parentWork.getWorkTypeId())
                                        .build();
                            }
                    );
        }

        // update all authorization
        updateWorkAuthorization(updatedWork);

        //update domain statistic
        domainService.updateDomainStatistics(updatedWork.getDomainId());
        log.info("Work '{}' has been updated by '{}'", updatedWork.getId(), updatedWork.getLastModifiedBy());
    }


    /**
     * Validate the location for the domain
     * check if the location belong to the source domain
     *
     * @param domainId   the domain id
     * @param locationId the id of the location
     * @param errorCode  the error code
     */
    private void validateLocationForDomain(String domainId, String locationId, int errorCode) {
        var location = wrapCatch(() -> locationService.findById(domainId, locationId), errorCode);
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
     * @param domainId    the domain id
     * @param shopGroupId the id of the location
     * @param errorCode   the error code
     */
    private void validateShopGroupForDomain(String domainId, String shopGroupId, int errorCode) {
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
     * Update a work workflow
     * <p>
     * it takes care of updating the workflow of the work
     *
     * @param newWorkValidation the id of the domain
     */
    public void isValidForWorkflow(String domainId, NewWorkValidation newWorkValidation) {
        Set<ConstraintViolation<WorkflowValidation<NewWorkValidation>>> violations = null;
        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, newWorkValidation.getNewWorkDTO().workTypeId());
        CompletableFuture<Void> scriptResult = scriptService.executeScriptFile(
                newWorkValidation.getWorkType().getValidatorName(),
                WorkTypeValidation.class,
                "checkValid",
                newWorkValidation);
        scriptResult.join();
    }

    /**
     * Update a work workflow
     * <p>
     * it takes care of updating the workflow of the work
     *
     * @param updateWorkValidation the information to validate
     */
    public void isValidForWorkflow(UpdateWorkValidation updateWorkValidation) {
        Set<ConstraintViolation<WorkflowValidation<UpdateWorkValidation>>> violations = null;
        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(updateWorkValidation.getExistingWork().getDomainId(), updateWorkValidation.getExistingWork().getWorkTypeId());
        CompletableFuture<Void> scriptResult = scriptService.executeScriptFile(
                updateWorkValidation.getWorkType().getValidatorName(),
                WorkTypeValidation.class,
                "checkValid",
                updateWorkValidation);
        scriptResult.join();
    }

    /**
     * Update a work workflow
     * <p>
     * it takes care of updating the workflow of the work
     *
     * @param domainId the id of the domain
     * @param work     the work to update
     */
    public void updateWorkWorkflow(String domainId, Work work, WorkType workType, UpdateWorkflowState updateState) {
        if (work == null) {
            return;
        }
        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, work.getWorkTypeId());
        // update workflow
        scriptService.executeScriptFile(
                workType.getValidatorName(),
                WorkTypeValidation.class,
                "updateWorkflow",
                WorkflowWorkUpdate.builder().work(work).workType(workType).workflow(wInstance).updateWorkflowState(updateState).build()).join();
    }

    /**
     * Update all the parent workflow of a work
     *
     * @param domainId    the id of the domain
     * @param parentWWork the parent work
     */
    public void updateParentWorkWorkflow(String domainId, Work parentWWork, WorkType workType) {
        if (domainId == null || parentWWork == null) {
            return;
        }

        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, parentWWork.getWorkTypeId());
        // update workflow
        scriptService.executeScriptFile(
                    workType.getValidatorName(),
                    WorkTypeValidation.class,
                    "updateWorkflow",
                    WorkflowWorkUpdate.builder().work(parentWWork).workType(workType).workflow(wInstance).build()).join();

        // save parent work with updated workflow
        wrapCatch(
                () -> workRepository.save(parentWWork),
                -2
        );
        if (parentWWork.getParentWorkId() != null) {
            // try to find the parent work
            var parentWork = wrapCatch(
                    () -> workRepository.findByDomainIdAndId(domainId, parentWWork.getParentWorkId()).orElseThrow(
                            () -> WorkNotFound
                                    .notFoundById()
                                    .errorCode(-1)
                                    .workId(parentWWork.getParentWorkId())
                                    .build()
                    ),
                    -3
            );

            // find work type of the parent and if found update the parent workflow
            workTypeRepository.findByDomainIdAndId(domainId, parentWork.getWorkTypeId())
                    .ifPresentOrElse(
                            wt -> updateParentWorkWorkflow(domainId, parentWork, wt),
                            () -> {
                                throw WorkTypeNotFound.notFoundById()
                                        .workId(parentWork.getWorkTypeId())
                                        .build();
                            }
                    );
//            // update ancestor workflow recursively
//            updateParentWorkWorkflow(domainId, parentWork);
        }
    }

    /**
     * Check if the user can update the work
     *
     * @param userId        the id of the user
     * @param domainId      the id of the domain
     * @param workId        the id of the work
     * @param updateWorkDTO the DTO to update the work
     */
    public void checkWorkflowForUpdate(String userId, String domainId, String workId, UpdateWorkDTO updateWorkDTO) {
        var work = workRepository
                .findByDomainIdAndId(domainId, workId)
                .orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                );
        var workflowInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, work.getWorkTypeId());
        workflowInstance.canUpdate(userId, work);
    }

    /**
     * Check if the user can create a new work
     *
     * @param domainId     the id of the domain
     * @param parentWorkId the id of the parent work
     * @return the parent work
     */
    public Work checkParentWorkflowForChild(String domainId, String parentWorkId) {
        if (domainId == null || parentWorkId == null) {
            return null;
        }
        // get parent work
        var foundParentWork = wrapCatch(
                () -> workRepository
                        .findByDomainIdAndId(domainId, parentWorkId)
                        .orElseThrow(
                                () -> WorkNotFound
                                        .notFoundById()
                                        .errorCode(-1)
                                        .workId(parentWorkId)
                                        .build()
                        ),
                -1
        );
        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, foundParentWork.getWorkTypeId());
        // update workflow
        assertion(
                WorkCannotHaveChildren
                        .byId()
                        .errorCode(-2)
                        .build(),
                () -> wInstance.canCreateChild(foundParentWork)
        );
        return foundParentWork;
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
        LocationDTO locationDTO = locationService.findById(work.getDomainId(), work.getLocationId());
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
     * Return the child of a work by his id
     *
     * @param domainId             the id of the domain
     * @param workId               the id of the work
     * @param workDetailsOptionDTO the option to retrieve the work
     * @return the work
     */
    public List<WorkDTO> findWorkChildrenById(@NotNull String domainId, @NotNull String workId, @Valid WorkDetailsOptionDTO workDetailsOptionDTO) {
        return wrapCatch(
                () -> workRepository.findByDomainIdAndParentWorkId(domainId, workId)
                        .stream()
                        .map(w -> workMapper.toDTO(w, workDetailsOptionDTO))
                        .toList(),
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
