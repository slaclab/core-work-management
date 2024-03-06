package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.ActivityNotFound;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO.User;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_FAKE_USER_TEMPLATE;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service
@Log4j2
@Validated
@AllArgsConstructor
public class WorkService {
    AuthService authService;
    WorkMapper workMapper;
    WorkRepository workRepository;
    WorkTypeRepository workTypeRepository;
    ActivityTypeRepository activityTypeRepository;
    ActivityRepository activityRepository;
    LocationService locationService;

    /**
     * Create a new work type
     *
     * @param newWorkTypeDTO the DTO to create the work type
     * @return the id of the created work type
     */
    public String ensureWorkType(@Valid NewWorkTypeDTO newWorkTypeDTO) {
        return wrapCatch(
                () -> workTypeRepository.ensureWorkType(
                        workMapper.toModel(newWorkTypeDTO)
                ),
                -1
        );
    }

    /**
     * Ensure work types
     */
    @Transactional
    public List<String> ensureWorkTypes(List<NewWorkTypeDTO> newWorkTypeDTOs) {
        List<String> listIds = new ArrayList<>();
        newWorkTypeDTOs.forEach(
                wt -> listIds.add(ensureWorkType(wt))
        );
        return listIds;
    }

    /**
     * Create a new activity type
     *
     * @param newActivityTypeDTO the DTO to create the activity type
     */
    public String ensureActivityType(@Valid NewActivityTypeDTO newActivityTypeDTO) {
        return wrapCatch(
                () -> activityTypeRepository.ensureActivityType(
                        workMapper.toModel(newActivityTypeDTO)
                ),
                -1
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
                () -> workTypeRepository.findAll(),
                -1
        );
        return workTypeList.stream().map(workMapper::toDTO).toList();
    }

    /**
     * Create a new work
     *
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    @Transactional
    public String createNew(NewWorkDTO newWorkDTO) {
        // contain the set of all user that will become admin for this new work
        Work workToSave = workMapper.toModel(newWorkDTO);
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -1
        );
        log.info("New Work '{}' has been created by '{}'", savedWork.getTitle(), savedWork.getCreatedBy());
        updateWorkAuthorization(savedWork);
        return savedWork.getId();
    }

    /**
     * Update a work
     *
     * @param workId         the id of the work
     * @param updateWorkDTO the DTO to update the work
     */
    @Transactional
    public void update(String workId, @Valid UpdateWorkDTO updateWorkDTO) {
        // fetch stored work to check if the work exists
        Work storedWork = wrapCatch(
                ()->workRepository.findById(workId).orElseThrow(
                        ()-> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workId)
                                .build()
                ),
                -2
        );


        // update the model
        workMapper.updateModel(updateWorkDTO, storedWork);
        updateWorkAuthorization(storedWork);
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
        if(authentication!=null) {
            // the creator is a writer
            writerUserList.add(work.getCreatedBy());
        }

        // authorize location manager as admin
        adminUserList.add(locationDTO.locationManagerUserId());
        // add shop group as writer in the form of virtual user
        writerUserList.add(SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(work.getShopGroupId()));
        // add assigned to users
        if(work.getAssignedTo() != null) {
            writerUserList.addAll(work.getAssignedTo());
        }

        // some user for various reason could be either admin and read
        // so removing the common from the reader list we are going
        // to give to the user only the higher permission
        // so remove all the admin that are also reader
        writerUserList.removeAll(adminUserList);

        adminUserList.forEach(
                (user)->{
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
                (user)->{
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
     * @param workId     the id of the work
     * @param reviewWorkDTO the DTO to close the work
     */
    public void reviewWork(String workId, ReviewWorkDTO reviewWorkDTO) {
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
                () -> work.getCurrentStatus().getStatus()== WorkStatus.Review,
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
     * Create a new activity
     *
     * @param workId         the id of the work
     * @param newActivityDTO the DTO to create the activity
     * @return the id of the created activity
     */
    @Transactional
    public String createNew(String workId, @Valid NewActivityDTO newActivityDTO) {
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

        var newActivity = workMapper.toModel(newActivityDTO, workId);
        var savedActivity = wrapCatch(
                () -> activityRepository.save(newActivity),
                -2
        );

        // fetch all activity status for work
        var activityStatusList = wrapCatch(
                () -> activityRepository.findAllActivityStatusByWorkId(workId),
                -3
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
                -4
        );
        log.info("New Activity '{}' has been added to work '{}'", savedActivity.getTitle(), work.getTitle());
        return savedActivity.getId();
    }

    /**
     * Update an activity
     *
     * @param workId the id of the work
     * @param activityId the id of the activity
     * @param updateActivityDTO the DTO to update the activity
     */
    public void update(String workId, String activityId, UpdateActivityDTO updateActivityDTO) {
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
     * Return the activity type by his id
     *
     * @param activityTypeId the id of the activity type
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
     * @param workId the id of the work
     * @param activityID the id of the activity
     * @param updateActivityStatusDTO the DTO to update the activity status
     */
    @Transactional
    public void setActivityStatus(String workId, String activityID,  UpdateActivityStatusDTO updateActivityStatusDTO) {
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
     * @return the found activities
     */
    public List<ActivityDTO> searchAllActivities(ActivityQueryParameterDTO activityQueryParameterDTO) {
        var activittList = wrapCatch(
                () -> activityRepository.searchAll(workMapper.toModel(activityQueryParameterDTO)),
                -1
        );
        return activittList.stream().map(workMapper::toDTO).toList();
    }
}
