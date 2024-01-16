package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.ActivityNotFound;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
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
        return savedWork.getId();
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
                        .collect(Collectors.toSet()),
                savedActivity.getCreatedBy());

        // save work and unlock
        wrapCatch(
                () -> workRepository.save(work),
                -4
        );
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
}
