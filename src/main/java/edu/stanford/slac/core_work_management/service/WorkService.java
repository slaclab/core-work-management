package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.ActivityNotFound;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatus;
import edu.stanford.slac.core_work_management.model.WorkStatusLog;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@Log4j2
@Validated
@AllArgsConstructor
public class WorkService {
    WorkMapper workMapper;
    WorkRepository workRepository;
    WorkTypeRepository workTypeRepository;
    ActivityTypeRepository activityTypeRepository;
    ActivityRepository activityRepository;

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
     * Create a new activity type
     *
     * @param workId             the id of the work
     * @param newActivityTypeDTO the DTO to create the activity type
     */
    public String ensureActivityType(String workId, @Valid NewActivityTypeDTO newActivityTypeDTO) {
        assertion(
                () -> workTypeRepository.existsById(workId),
                WorkNotFound.notFoundById().errorCode(-1).workId(workId).build()
        );
        return wrapCatch(
                () -> activityTypeRepository.ensureActivityType(
                        workId,
                        workMapper.toModel(newActivityTypeDTO)
                ),
                -1
        );
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
     * Return all the work types
     *
     * @return the list of work types
     */
    public List<ActivityTypeDTO> findAllActivityTypesByWorkId(String workId) {
        var workTypeList = wrapCatch(
                () -> activityTypeRepository.findAll(),
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
    public String createNew(NewWorkDTO newWorkDTO) {
        Work workToSave = workMapper.toModel(newWorkDTO);
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -1
        );
        log.info("New Work '{}' has been created by '{}'", savedWork.getTitle(), savedWork.getCreatedBy());
        return savedWork.getId();
    }

    /**
     * Close a work
     *
     * @param workId     the id of the work
     * @param closeWorkDTO the DTO to close the work
     */
    public void closeWork(String workId, CloseWorkDTO closeWorkDTO) {
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
        work.setFollowupDescriptionOnClose(closeWorkDTO.followUpDescription());
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
}
