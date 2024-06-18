package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.validation.ModelFieldValidationService;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.swing.text.html.Option;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service
@Log4j2
@Validated
@RequiredArgsConstructor
public class WorkService {
    private final DomainService domainService;
    private final AuthService authService;
    private final WorkMapper workMapper;
    private final WorkRepository workRepository;
    private final WorkTypeRepository workTypeRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final ActivityRepository activityRepository;
    private final LogService logService;
    private final LocationService locationService;
    private final ShopGroupService shopGroupService;
    private final ModelFieldValidationService modelFieldValidationService;
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    /**
     * Create a new work type
     *
     * @param newWorkTypeDTO the DTO to create the work type
     * @return the id of the created work type
     */
    public String ensureWorkType(@Valid NewWorkTypeDTO newWorkTypeDTO) {
        List<WATypeCustomFieldDTO> normalizedCustomField = new ArrayList<>();
        newWorkTypeDTO.customFields().forEach(
                (customField) -> {
                    var tmpName = customField.name();
                    normalizedCustomField.add(
                            customField.toBuilder()
                                    .id(UUID.randomUUID().toString())
                                    .label(tmpName)
                                    .name(StringUtility.toCamelCase(tmpName))
                                    .build()
                    );
                }
        );
        var normalizedActivityDTO = newWorkTypeDTO.toBuilder().customFields(normalizedCustomField).build();
        return wrapCatch(
                () -> workTypeRepository.ensureWorkType(
                        workMapper.toModel(newWorkTypeDTO)
                ),
                -1
        );
    }

    /**
     * create a new activity type
     *
     * @param newWorkTypeDTO the new work type to create
     * @return the activity id
     */
    public String createNew(@Valid NewWorkTypeDTO newWorkTypeDTO) {
        // set the id of the custom attributes
        var toSave = workMapper.toModel(newWorkTypeDTO);
        toSave.getCustomFields().forEach(
                (customField) -> {
                    customField.setId(UUID.randomUUID().toString());
                    customField.setName(
                            customField.getLabel() == null ?
                                    StringUtility.toCamelCase(customField.getName()) :
                                    StringUtility.toCamelCase(customField.getLabel())
                    );
                    customField.setLabel(customField.getLabel());
                }
        );
        return wrapCatch(
                () -> workTypeRepository.save(toSave),
                -1
        ).getId();
    }

    /**
     * Return the work type  by his id
     *
     * @param id the id of the activity type
     * @return the activity type
     */
    public WorkTypeDTO findWorkTypeById(String id) {
        return wrapCatch(
                () -> workTypeRepository.findById(
                        id
                ),
                -1
        ).map(workMapper::toDTO).orElseThrow(
                () -> WorkTypeNotFound
                        .notFoundById()
                        .errorCode(-2)
                        .workId(id)
                        .build()
        );
    }

    /**
     * Create a new activity type
     *
     * @param newActivityTypeDTO the DTO to create the activity type
     */
    public String ensureActivityType(@Valid NewActivityTypeDTO newActivityTypeDTO) {
        // set the id of the custom attributes
        List<WATypeCustomFieldDTO> normalizedCustomField = new ArrayList<>();
        newActivityTypeDTO.customFields().forEach(
                (customField) -> {
                    var tmpName = customField.name();
                    normalizedCustomField.add(
                            customField.toBuilder()
                                    .id(UUID.randomUUID().toString())
                                    .label(tmpName)
                                    .name(StringUtility.toCamelCase(tmpName))
                                    .build()
                    );
                }
        );
        var normalizedActivityDTO = newActivityTypeDTO.toBuilder().customFields(normalizedCustomField).build();
        return wrapCatch(
                () -> activityTypeRepository.ensureActivityType(
                        workMapper.toModel(normalizedActivityDTO)
                ),
                -1
        );
    }

    /**
     * create a new activity type
     *
     * @param newActivityTypeDTO the new activity to create
     * @return the activity id
     */
    public String createNew(@Valid NewActivityTypeDTO newActivityTypeDTO) {
        // set the id of the custom attributes
        var toSave = workMapper.toModel(newActivityTypeDTO);
        toSave.getCustomFields().forEach(
                (customField) -> {
                    customField.setId(UUID.randomUUID().toString());
                    customField.setName(
                            customField.getLabel() == null ?
                                    StringUtility.toCamelCase(customField.getName()) :
                                    StringUtility.toCamelCase(customField.getLabel())
                    );
                    customField.setLabel(customField.getLabel());
                }
        );
        return wrapCatch(
                () -> activityTypeRepository.save(toSave),
                -1
        ).getId();
    }

    /**
     * Return the activity type by his id
     *
     * @param id the id of the activity type
     * @return the activity type
     */
    public ActivityTypeDTO findActivityTypeById(String id) {
        return wrapCatch(
                () -> activityTypeRepository.findById(
                        id
                ),
                -1
        ).map(workMapper::toDTO).orElseThrow(
                () -> ActivityTypeNotFound
                        .notFoundById()
                        .errorCode(-2)
                        .activityTypeId(id)
                        .build()
        );
    }

    /**
     * Update an activity type
     *
     * @param activityId            the id of the activity type
     * @param updateActivityTypeDTO the DTO to update the activity type
     */
    public void updateActivityType(String activityId, UpdateActivityTypeDTO updateActivityTypeDTO) {
        var activityType = wrapCatch(
                () -> activityTypeRepository.findById(activityId).orElseThrow(
                        () -> ActivityTypeNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .activityTypeId(activityId)
                                .build()
                ),
                -1
        );
        var updatedActivityTypeModel = workMapper.updateModel(updateActivityTypeDTO, activityType);
        wrapCatch(
                () -> activityTypeRepository.save(updatedActivityTypeModel),
                -2
        );
    }

    /**
     * Ensure activity types
     */
    public List<String> ensureActivitiesTypes(List<NewActivityTypeDTO> newActivityTypeDTOS) {
        List<String> listIds = new ArrayList<>();
        newActivityTypeDTOS.forEach(
                at -> listIds.add(ensureActivityType(at))
        );
        return listIds;
    }


    /**
     * Return all the work types
     *
     * @return the list of work types
     */
    public List<WorkTypeDTO> findAllWorkTypes() {
        var workTypeList = wrapCatch(
                workTypeRepository::findAll,
                -1
        );
        return workTypeList.stream().map(workMapper::toDTO).toList();
    }

    /**
     * Return all the activity types
     *
     * @return the list of activity types
     */
    public List<ActivityTypeDTO> findAllActivityTypes() {
        var workTypeList = wrapCatch(
                activityTypeRepository::findAll,
                -1
        );
        return workTypeList.stream().map(workMapper::toDTO).toList();
    }

    /**
     * Return all the activity subtypes
     *
     * @return the list of activity subtypes
     */
    public List<ActivityTypeSubtypeDTO> findAllActivitySubTypes() {
        return List.of(
                ActivityTypeSubtypeDTO.BugFix,
                ActivityTypeSubtypeDTO.DeferredRepair,
                ActivityTypeSubtypeDTO.Enhancement,
                ActivityTypeSubtypeDTO.Fabrication,
                ActivityTypeSubtypeDTO.Inspection,
                ActivityTypeSubtypeDTO.Installation,
                ActivityTypeSubtypeDTO.Maintenance,
                ActivityTypeSubtypeDTO.NewApplication,
                ActivityTypeSubtypeDTO.Safety,
                ActivityTypeSubtypeDTO.SoftwareRelease,
                ActivityTypeSubtypeDTO.Other
        );
    }

    /**
     * Create a new work automatically creating the sequence
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    public String createNew(@Valid NewWorkDTO newWorkDTO) {
        // contain the set of all user that will become admin for this new work
        Long newWorkSequenceId = wrapCatch(
                workRepository::getNextWorkId,
                -1
        );
        return createNew(newWorkSequenceId, newWorkDTO, Optional.of(false));
    }

    /**
     * Create a new work automatically creating the sequence
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    public String createNew(@Valid NewWorkDTO newWorkDTO, Optional<Boolean> logIf) {
        // contain the set of all user that will become admin for this new work
        Long newWorkSequenceId = wrapCatch(
                workRepository::getNextWorkId,
                -1
        );
        return createNew(newWorkSequenceId, newWorkDTO, logIf);
    }

    /**
     * Create a new work
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    @Transactional
    public String createNew(Long workSequence, @Valid NewWorkDTO newWorkDTO, Optional<Boolean> logIf) {
        //check if the domain exists
        assertion(
                DomainNotFound
                        .notFoundById()
                        .errorCode(-1)
                        .id(newWorkDTO.domainId())
                        .build(),
                () -> domainService.existsById(newWorkDTO.domainId())
        );
        WorkType workType = wrapCatch(
                () -> workTypeRepository
                        .findById(newWorkDTO.workTypeId())
                        .orElseThrow(
                                () -> WorkTypeNotFound
                                        .notFoundById()
                                        .errorCode(-1)
                                        .workId(newWorkDTO.workTypeId())
                                        .build()
                        ),
                -2
        );
        // validate lov
        modelFieldValidationService.verify(
                newWorkDTO,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );

        // contain the set of all user that will become admin for this new work
        Work workToSave = workMapper.toModel(
                workSequence,
                newWorkDTO
        );

        // validate location and group shop against the domain
        validateLocationForDomain(workToSave.getLocationId(), workToSave.getDomainId(), -3);
        validateShopGroupForDomain(workToSave.getShopGroupId(), workToSave.getDomainId(), -4);
        // save work
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -5
        );

        log.info("New Work '{}' has been created by '{}'", savedWork.getTitle(), savedWork.getCreatedBy());
        updateWorkAuthorization(savedWork);
        log.info("Update domain statistic");
        domainService.updateDomainStatistics(savedWork.getDomainId());

        // log the creation of the work
        if (logIf.isPresent() && logIf.get()) {
            logService.createNewLogEntry
                    (
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
     * @param locationId the id of the location
     * @param srcDomain  the domain id
     * @param errorCode  the error code
     */
    private void validateLocationForDomain(String locationId, String srcDomain, int errorCode) {
        var location = wrapCatch(() -> locationService.findById(locationId), errorCode);
        assertion(
                InvalidLocation
                        .byLocationNameDomainId()
                        .errorCode(errorCode)
                        .locationName(location.name())
                        .domainId(srcDomain)
                        .build(),
                () -> (location.domain().id().compareTo(srcDomain) == 0)
        );
    }

    /**
     * Validate shop group for the domain
     * check if the shop group belong to the source domain
     *
     * @param shopGroupId the id of the location
     * @param srcDomain   the domain id
     * @param errorCode   the error code
     */
    private void validateShopGroupForDomain(String shopGroupId, String srcDomain, int errorCode) {
        var shopGroup = wrapCatch(() -> shopGroupService.findById(shopGroupId), errorCode);
        assertion(
                InvalidShopGroup
                        .byShopGroupNameDomainId()
                        .errorCode(errorCode)
                        .shopGroupName(shopGroup.name())
                        .domainId(srcDomain)
                        .build(),
                () -> (shopGroup.domain().id().compareTo(srcDomain) == 0)
        );
    }

    /**
     * Update a work
     *
     * @param workId        the id of the work
     * @param updateWorkDTO the DTO to update the work
     */
    @Transactional
    public void update(String workId, @Valid UpdateWorkDTO updateWorkDTO) {
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

        // validate lov
        modelFieldValidationService.verify(
                updateWorkDTO,
                Objects.requireNonNullElse(workType.getCustomFields(), emptyList())
        );

        // check that all the user in the assigned to are listed into the shop group
        if (updateWorkDTO.assignedTo() != null) {
            updateWorkDTO.assignedTo().forEach(
                    (user) -> {
                        assertion(
                                () -> shopGroupService.checkContainsAUserEmail(foundWork.getShopGroupId(), user),
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
     * @param workId        the id of the work
     * @param reviewWorkDTO the DTO to close the work
     */
    public void reviewWork(String workId, @Valid ReviewWorkDTO reviewWorkDTO) {
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
        // check for work status
        assertion(
                () -> work.getCurrentStatus().getStatus() == WorkStatus.Review,
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
                        .status(WorkStatus.Closed)
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
    public WorkDTO findWorkById(String id) {
        return wrapCatch(
                () -> workRepository.findById(id).map(workMapper::toDTO).orElseThrow(
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
     * Create a new activity automatically generating the next activity id
     *
     * @param workId         the id of the work
     * @param newActivityDTO the DTO to create the activity
     * @return the id of the created activity
     */
    public String createNew(@NotNull String workId, @Valid NewActivityDTO newActivityDTO) {
        return createNew(workId, workRepository.getNextActivityNumber(workId), newActivityDTO);
    }

    /**
     * Create a new activity
     *
     * @param workId              the id of the work
     * @param nextActivityNumbers the next activity number
     * @param newActivityDTO      the DTO to create the activity
     * @return the id of the created activity
     */
    @Transactional
    public String createNew(@NotNull String workId, @NotNull Long nextActivityNumbers, @Valid NewActivityDTO newActivityDTO) {
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

        var activityType = wrapCatch(
                () -> activityTypeRepository.findById(newActivityDTO.activityTypeId()).orElseThrow(
                        () -> ActivityTypeNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .activityTypeId(newActivityDTO.activityTypeId())
                                .build()
                ),
                -3
        );

        //validate location and shop group against domain
        if (newActivityDTO.locationId() != null) {
            validateLocationForDomain(newActivityDTO.locationId(), work.getDomainId(), -3);
        }
        if (newActivityDTO.shopGroupId() != null) {
            validateShopGroupForDomain(newActivityDTO.shopGroupId(), work.getDomainId(), -4);
        }

        // validate model custom attributes
        modelFieldValidationService.verify(
                newActivityDTO,
                Objects.requireNonNullElse(activityType.getCustomFields(), emptyList())
        );

        // convert to model
        var newActivity = workMapper.toModel(newActivityDTO, workId, work.getWorkNumber(), work.getDomainId(), nextActivityNumbers);

        var savedActivity = wrapCatch(
                () -> activityRepository.save(newActivity),
                -5
        );

        // fetch all activity status for work
        var activityStatusList = wrapCatch(
                () -> activityRepository.findAllActivityStatusByWorkId(workId),
                -6
        );

        // update the work status
        work.updateStatus(
                activityStatusList
                        .stream()
                        .map(ActivityStatusLog::getStatus)
                        .collect(Collectors.toSet())
        );

        // save work and unlock
        wrapCatch(
                () -> workRepository.save(work),
                -7
        );
        log.info("New Activity '{}' has been added to work '{}'", savedActivity.getTitle(), work.getTitle());
        //update domain statistic
        domainService.updateDomainStatistics(savedActivity.getDomainId());
        return savedActivity.getId();
    }

    /**
     * Update an activity
     *
     * @param workId            the id of the work
     * @param activityId        the id of the activity
     * @param updateActivityDTO the DTO to update the activity
     */
    public void update(@NotNull String workId, @NotNull String activityId, @Valid UpdateActivityDTO updateActivityDTO) {
        // check for work existence
        assertion(
                () -> workRepository.existsById(workId),
                WorkNotFound
                        .notFoundById()
                        .errorCode(-1)
                        .workId(workId)
                        .build()
        );
        // check for activity
        var activityStored = wrapCatch(
                () -> activityRepository.findById(activityId).orElseThrow(
                        () -> ActivityNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .activityId(activityId)
                                .build()
                ),
                -2
        );


        //fetch the whole activity type
        var activityType = wrapCatch(
                () -> activityTypeRepository.findById(activityStored.getActivityTypeId()).orElseThrow(
                        () -> ActivityTypeNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .activityTypeId(activityStored.getActivityTypeId())
                                .build()
                ),
                -3
        );

        //TODO validate location and shop group against domain
        if (updateActivityDTO.locationId() != null) {
            validateLocationForDomain(updateActivityDTO.locationId(), activityStored.getDomainId(), -3);
        }
        if (updateActivityDTO.shopGroupId() != null) {
            validateShopGroupForDomain(updateActivityDTO.shopGroupId(), activityStored.getDomainId(), -4);
        }

        // validate model attribute
        modelFieldValidationService.verify(
                updateActivityDTO,
                Objects.requireNonNullElse(activityType.getCustomFields(), emptyList())
        );

        // assert that activity need to be related to the work
        assertion(
                () -> workId.equals(activityStored.getWorkId()),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("The activity does not belong to the work")
                        .errorDomain("WorkService::update(String,String,UpdateActivityDTO)")
                        .build()
        );

        // update the model
        workMapper.updateModel(updateActivityDTO, activityStored);

        // save the activity
        var savedActivity = wrapCatch(
                () -> activityRepository.save(activityStored),
                -2
        );
        log.info("Activity '{}' has been updated by '{}'", savedActivity.getId(), savedActivity.getLastModifiedBy());
        //update domain statistic
        domainService.updateDomainStatistics(activityStored.getDomainId());
    }

    /**
     * Return the activity by his id
     *
     * @param activityId the id of the activity
     * @return the activity
     */
    public ActivityDTO findActivityById(String activityId) {
        return wrapCatch(
                () -> activityRepository.findById(activityId).map(workMapper::toDTO).orElseThrow(
                        () -> ActivityNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .activityId(activityId)
                                .build()
                ),
                -1
        );
    }

    /**
     * Return all the activities by the work id
     *
     * @param workId the id of the work
     * @return the list of activities
     */
    public List<ActivitySummaryDTO> findAllActivitiesByWorkId(String workId) {
        return wrapCatch(
                () -> activityRepository.findAllByWorkId(workId)
                        .stream()
                        .map(workMapper::toSummaryDTO)
                        .toList(),
                -1
        );
    }

    /**
     * Return the activity type by his id
     *
     * @param activity the id of the activity type
     * @return the activity type
     */
    public List<ActivityStatusDTO> getPermittedStatus(ActivityStatusDTO activity) {
        ActivityStatusStateMachine activityStatusStateMachine = new ActivityStatusStateMachine();
        return activityStatusStateMachine.getAvailableState(workMapper.toModel(activity))
                .stream()
                .map(workMapper::toDTO)
                .toList();
    }

    /**
     * Change the status of an activity
     *
     * @param workId                  the id of the work
     * @param activityID              the id of the activity
     * @param updateActivityStatusDTO the DTO to update the activity status
     */
    @Transactional
    public void setActivityStatus(String workId, String activityID, UpdateActivityStatusDTO updateActivityStatusDTO) {
        // check for work existence
        var workFound = wrapCatch(
                () -> workRepository.findById(workId).orElseThrow(
                        () -> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                ),
                -1
        );
        // check for activity
        var activityFound = wrapCatch(
                () -> activityRepository.findById(activityID).orElseThrow(
                        () -> ActivityNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .activityId(activityID)
                                .build()
                ),
                -2
        );
        // assert that activity need to be related to the work
        assertion(
                () -> workFound.getId().equals(activityFound.getWorkId()),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("The activity does not belong to the work")
                        .errorDomain("WorkService::changeActivityStatus")
                        .build()
        );
        // switch to current status. internal is checked the validity of the transition
        activityFound.setStatus(
                workMapper.toModel(updateActivityStatusDTO.newStatus()),
                updateActivityStatusDTO.followupDescription()
        );
        // save the activity
        var savedActivity = wrapCatch(
                () -> activityRepository.save(activityFound),
                -2
        );
        log.info("Activity '{}' has change his status to '{}' by '{}'", savedActivity.getId(), savedActivity.getCurrentStatus().getStatus(), savedActivity.getCurrentStatus().getChanged_by());
        // fetch all activity status for work
        var activityStatusList = wrapCatch(
                () -> activityRepository.findAllActivityStatusByWorkId(workId),
                -3
        );

        // update the work status
        workFound.updateStatus(
                activityStatusList
                        .stream()
                        .map(ActivityStatusLog::getStatus)
                        .collect(Collectors.toSet())
        );

        // save work and unlock
        var savedWork = wrapCatch(
                () -> workRepository.save(workFound),
                -4
        );
        log.info("Work '{}' has change his status to status '{}'", savedWork.getId(), savedWork.getCurrentStatus().getStatus());
        domainService.updateDomainStatistics(activityFound.getDomainId());
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
        return workList.stream().map(workMapper::toDTO).toList();
    }

    /**
     * Search on all the activities
     *
     * @return the found activities
     */
    public List<ActivityDTO> searchAllActivities(ActivityQueryParameterDTO activityQueryParameterDTO) {
        var activittList = wrapCatch(
                () -> activityRepository.searchAll(workMapper.toModel(activityQueryParameterDTO)),
                -1
        );
        return activittList.stream().map(workMapper::toDTO).toList();
    }

    @Cacheable("work-authorization")
    public String getCachedData() {
        return "cached";
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
