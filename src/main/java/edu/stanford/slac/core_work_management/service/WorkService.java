package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.*;
import edu.stanford.slac.core_work_management.exception.*;
import edu.stanford.slac.core_work_management.model.*;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

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
    private final LocationMapper locationMapper;
    private final ShopGroupMapper shopGroupMapper;

    private final ScriptService scriptService;
    private final DomainService domainService;
    private final BucketService bucketService;
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
    private final LocationMapperImpl locationMapperImpl;

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
                        .findByDomainIdAndId(domainId, newWorkDTO.workTypeId())
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
            parentWork = checkParentWorkflowForChild(domainId, newWorkDTO);
        } else {
            parentWork = null;
        }

        // contain the set of all user that will become admin for this new work
        Work workToSave = workMapper.toModel(
                domainId,
                workSequence,
                domainMapper.toEmbeddable(workType),
                newWorkDTO
        );

        // validate lov
        modelFieldValidationService.verify(
                workToSave,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );

        // validate location and group shop against the domain
        if (newWorkDTO.locationId() != null) {
            workToSave.setLocation(
                    locationMapper.toEmbeddable(locationService.findById(domainId, newWorkDTO.locationId()))
            );
        }
        if (newWorkDTO.shopGroupId() != null) {
            workToSave.setShopGroup(
                    shopGroupMapper.toEmbeddable(shopGroupService.findByDomainIdAndId(domainId, newWorkDTO.shopGroupId()))
            );
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
            updateParentWorkWorkflow(parentWork);
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

        // update the model
        workMapper.updateModel(updateWorkDTO, foundWork);

        // check if the new work that is being created is valid for the workflow
        // we send the updated work to the validator
        isValidForWorkflow(
                UpdateWorkValidation
                        .builder()
                        .updateWorkDTO(updateWorkDTO)
                        .existingWork(foundWork)
                        .build()
        );


        // validate lov
        modelFieldValidationService.verify(
                foundWork,
                Objects.requireNonNullElse(foundWork.getWorkType().getCustomFields(), emptyList())
        );

        // validate location and group shop against the domain
        if (updateWorkDTO.locationId() != null) {
            foundWork.setLocation(
                    locationMapper.toEmbeddable(locationService.findById(domainId, updateWorkDTO.locationId()))
            );
        }

        // validate shop group against the domain
        if (updateWorkDTO.shopGroupId() != null) {
            shopGroupMapper.toEmbeddable(
                    shopGroupService.findByDomainIdAndId(domainId, updateWorkDTO.shopGroupId())
            );
        }

        // lastly we need to update the workflow
        updateWorkWorkflow(foundWork, domainMapper.toModel(updateWorkDTO.workflowStateUpdate()));

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
            // try to find parent work type
            updateParentWorkWorkflow(parentWork);
        }

        // update all authorization
        updateWorkAuthorization(updatedWork);

        //update domain statistic
        domainService.updateDomainStatistics(updatedWork.getDomainId());
        log.info("Work '{}' has been updated by '{}'", updatedWork.getId(), updatedWork.getLastModifiedBy());
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
        var shopGroupExists = wrapCatch(() -> shopGroupService.existsByDomainIdAndId(domainId, shopGroupId), errorCode);
        assertion(
                ShopGroupNotFound
                        .notFoundById()
                        .errorCode(errorCode)
                        .shopGroupId(shopGroupId)
                        .build(),
                () -> shopGroupExists
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
        WorkTypeValidation wtv = scriptService.getInterfaceImplementationFromFile(
                newWorkValidation.getWorkType().getValidatorName(),
                WorkTypeValidation.class
        );
        wtv.checkValid(newWorkValidation);
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
        WorkTypeValidation wtv = scriptService.getInterfaceImplementationFromFile(
                updateWorkValidation.getExistingWork().getWorkType().getValidatorName(),
                WorkTypeValidation.class
        );
        wtv.checkValid(updateWorkValidation);
    }

    /**
     * Update a work workflow
     * <p>
     * it takes care of updating the workflow of the work
     *
     * @param work the work to update
     */
    public void updateWorkWorkflow(Work work, UpdateWorkflowState updateState) {
        if (work == null || updateState == null) {
            return;
        }
        WorkTypeValidation wtv = scriptService.getInterfaceImplementationFromFile(
                work.getWorkType().getValidatorName(),
                WorkTypeValidation.class
        );
        // fetch the workflow
        var wInstance = (BaseWorkflow) applicationContext.getBean(work.getWorkType().getWorkflow().getImplementation());
        wtv.updateWorkflow(WorkflowWorkUpdate.builder().work(work).workflow(wInstance).updateWorkflowState(updateState).build());
    }

    /**
     * Update all the parent workflow of a work
     *
     * @param parentWWork the parent work
     */
    public void updateParentWorkWorkflow(Work parentWWork) {
        if (parentWWork == null) {
            return;
        }
        // get validator for the work type
        WorkTypeValidation wtv = scriptService.getInterfaceImplementationFromFile(
                parentWWork.getWorkType().getValidatorName(),
                WorkTypeValidation.class
        );
        // retrieve workflow instance
        var wInstance = (BaseWorkflow) applicationContext.getBean(parentWWork.getWorkType().getWorkflow().getImplementation());

        // update workflow with the script associated to the work type
        wtv.updateWorkflow(WorkflowWorkUpdate.builder().work(parentWWork).workflow(wInstance).build());

        // save parent work with updated workflow
        wrapCatch(
                () -> workRepository.save(parentWWork),
                -2
        );
        if (parentWWork.getParentWorkId() != null) {
            // try to find the parent work
            var parentWork = wrapCatch(
                    () -> workRepository.findByDomainIdAndId(parentWWork.getDomainId(), parentWWork.getParentWorkId()).orElseThrow(
                            () -> WorkNotFound
                                    .notFoundById()
                                    .errorCode(-1)
                                    .workId(parentWWork.getParentWorkId())
                                    .build()
                    ),
                    -3
            );

            // find work type of the parent and if found update the parent workflow
            updateParentWorkWorkflow(parentWork);
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
//    public void checkWorkflowForUpdate(String userId, String domainId, String workId, UpdateWorkDTO updateWorkDTO) {
//        var work = workRepository
//                .findByDomainIdAndId(domainId, workId)
//                .orElseThrow(
//                        () -> WorkNotFound
//                                .notFoundById()
//                                .errorCode(-1)
//                                .workId(workId)
//                                .build()
//                );
//        var workflowInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, work.getWorkTypeId());
//        workflowInstance.canUpdate(userId, work);
//        // retrieve workflow instance
//        var wInstance = domainService.getWorkflowInstanceByDomainIdAndWorkTypeId(domainId, parentWWork.getWorkTypeId());
//
//    }

    /**
     * Check if the user can create a new work
     *
     * @param domainId   the id of the domain
     * @param newWorkDTO the dto to create the work
     * @return the parent work
     */
    public Work checkParentWorkflowForChild(String domainId, @Valid NewWorkDTO newWorkDTO) {
        if (domainId == null || newWorkDTO == null || newWorkDTO.parentWorkId() == null) {
            return null;
        }
        // get parent work
        var foundParentWork = wrapCatch(
                () -> workRepository
                        .findByDomainIdAndId(domainId, newWorkDTO.parentWorkId())
                        .orElseThrow(
                                () -> WorkNotFound
                                        .notFoundById()
                                        .errorCode(-1)
                                        .workId(newWorkDTO.parentWorkId())
                                        .build()
                        ),
                -1
        );
        // check if parent work type admin the child WorkType id
        assertion(
                WorkCannotHaveChildren
                        .byId()
                        .errorCode(-2)
                        .workId(foundParentWork.getId())
                        .build(),
                () -> all(
                        () -> foundParentWork.getWorkType().getChildWorkTypeIds() != null,
                        () -> foundParentWork.getWorkType().getChildWorkTypeIds().contains(newWorkDTO.workTypeId())
                )
        );
        // fetch the workflow
        var wInstance = (BaseWorkflow) applicationContext.getBean(foundParentWork.getWorkType().getWorkflow().getImplementation());
        var validationInstance = scriptService.getInterfaceImplementationFromFile(
                foundParentWork.getWorkType().getValidatorName(),
                WorkTypeValidation.class
        );
        // validate the child admission
        validationInstance.admitChildren(AdmitChildrenValidation.builder().work(foundParentWork).workflow(wInstance).build());
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
        LocationDTO locationDTO = locationService.findById(work.getDomainId(), work.getLocation().getId());

        if (work.getCreatedBy() != null) {
            // the creator is a writer
            writerUserList.add(work.getCreatedBy());
        }

        // authorize location manager as admin
        adminUserList.add(locationDTO.locationManagerUserId());
        // add shop group as writer in the form of virtual user
        writerUserList.add(SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(work.getShopGroup().getId()));
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
     * Associate a work to a bucket slot
     *
     * @param domainId     the id of the domain
     * @param workId       the id of the work
     * @param bucketSlotId the id of the bucket slot
     */
    @Transactional
    public void associateWorkToBucketSlot(String domainId, String workId, String bucketSlotId, Optional<Boolean> move) {
        // check if bucket is present and get it
        BucketSlotDTO bucketFound = bucketService.findById(bucketSlotId);
        // check for domain
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("Bucket not admin the domainId")
                        .errorDomain("WorkService::associateWorkToBucketSlot")
                        .build(),
                // check if the domain admin the domain
                () -> bucketFound.domainIds().contains(domainId)
        );

        // check if the work exists
        var foundWork = wrapCatch(
                () -> workRepository.findByDomainIdAndId(domainId, workId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .workId(workId)
                                .build()
                ),
                -2
        );

        // check for work type admission
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("Bucket not admin the domainId")
                        .errorDomain("WorkService::associateWorkToBucketSlot")
                        .build(),
                // check if the domain admin the domain
                () -> bucketFound.admittedWorkType().stream().anyMatch(
                        wt -> wt.id().compareTo(foundWork.getWorkType().getId()) == 0 && wt.domainId().compareTo(foundWork.getDomainId()) == 0)
        );

        // check if we need to force the api to move to another bucket
        if (move.isEmpty() || !move.get()) {
            // check work is not already associated to other bucket
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-4)
                            .errorMessage("Work already associated to a bucket")
                            .errorDomain("WorkService::associateWorkToBucketSlot")
                            .build(),
                    () -> any(
                            // check if work is already associated to some bucket
                            () -> foundWork.getCurrentBucketAssociation() == null
                    )
            );
        }

        // check if work is active into another bucket
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-5)
                        .errorMessage("Work is already associated to the bucket")
                        .errorDomain("WorkService::associateWorkToBucketSlot")
                        .build(),
                () -> any(
                        // check if work is already associated to the target bucket
                        () -> any(
                                () -> foundWork.getCurrentBucketAssociation() == null,
                                () -> foundWork.getCurrentBucketAssociation().getBucketId().compareTo(bucketSlotId) != 0
                        )
                )
        );

        // at this point work is valid for the bucket ad we can associate them
        if (foundWork.getCurrentBucketAssociation() != null) {
            foundWork.getBucketAssociationsHistory().add(
                    foundWork.getCurrentBucketAssociation().toBuilder().rolled(true).build()
            );
        }
        foundWork.setCurrentBucketAssociation(
                WorkBucketAssociation
                        .builder()
                        .bucketId(bucketSlotId)
                        .rolled(false)
                        .build()
        );

        // lastly we need to update the workflow
        updateWorkWorkflow(
                foundWork,
                null);

        // save the bucket slot
        wrapCatch(
                () -> workRepository.save(foundWork),
                -6
        );
    }

    /**
     * Remove a work from a bucket slot
     *
     * @param domainId     the id of the domain
     * @param workId       the id of the work
     * @param bucketSlotId the id of the bucket slot
     */
    @Transactional
    public void removeWorkFromBucketSlot(String domainId, String workId, String bucketSlotId) {
        // check if the work exists
        var work = wrapCatch(
                () -> workRepository.findByDomainIdAndId(domainId, workId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .workId(workId)
                                .build()
                ),
                -2
        );

        // check if the work is associated to the bucket
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-4)
                        .errorMessage("Work is not associated to the bucket")
                        .errorDomain("WorkService::removeWorkFromBucketSlot")
                        .build(),
                // the work can be only removed if it's associated to the bucket
                () -> work.getCurrentBucketAssociation().getBucketId().compareTo(bucketSlotId) == 0
        );

        // remove the association
        work.getBucketAssociationsHistory().add(
                work.getCurrentBucketAssociation()
        );
        // clear current association
        work.setCurrentBucketAssociation(null);
        // save the work
        wrapCatch(
                () -> workRepository.save(work),
                -5
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
                () -> workRepository.findById(workId).map(w -> w.getShopGroup().getId()).orElseThrow(
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

    /**
     * Return all the work that belong to the bucket
     *
     * @param id the id of the work
     * @return the work
     */
    public List<WorkDTO> findAllByBucketId(String id) {
        return wrapCatch(
                () -> workRepository.findAllByCurrentBucketAssociationBucketIdIs(id)
                        .stream()
                        .map(w-> workMapper.toDTO(w, WorkDetailsOptionDTO.builder().build()))
                        .toList(),
                -1
        );
    }

    /**
     * Check if a work is associated to a bucket slot
     *
     * @param bucketId the id of the bucket
     * @return true if the work is associated to the bucket slot
     */
    public boolean existByBucketSlotId(String bucketId) {
        return wrapCatch(
                () -> workRepository.existsByCurrentBucketAssociationBucketId(bucketId),
                -1
        );
    }

}
