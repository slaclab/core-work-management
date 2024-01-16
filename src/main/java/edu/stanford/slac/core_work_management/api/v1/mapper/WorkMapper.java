package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper for the entity {@link Work}
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class WorkMapper {
    @Autowired
    WorkTypeRepository workTypeRepository;
    @Autowired
    ActivityTypeRepository activityTypeRepository;
    /**
     * Convert the {@link NewWorkTypeDTO} to a {@link WorkType}
     * @param newWorkTypeDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkType toModel(NewWorkTypeDTO newWorkTypeDTO);
    /**
     * Convert the {@link NewActivityTypeDTO} to a {@link ActivityType}
     * @param newActivityTypeDTO the DTO to convert
     * @return the converted work type
     */
    abstract public ActivityType toModel(NewActivityTypeDTO newActivityTypeDTO);
    /**
     * Convert the {@link NewWorkDTO} to a {@link Work}
     * @param newWorkDTO the DTO to convert
     * @return the converted entity
     */
    abstract public Work toModel(NewWorkDTO newWorkDTO);
    /**
     * Convert the {@link WorkType} to a {@link WorkTypeDTO}
     * @param workType the entity to convert
     * @return the converted DTO
     */
    abstract public WorkTypeDTO toDTO(WorkType workType);
    /**
     * Convert the {@link ActivityType} to a {@link ActivityTypeDTO}
     * @param activityType the entity to convert
     * @return the converted DTO
     */
    abstract public ActivityTypeDTO toDTO(ActivityType activityType);
    /**
     * Convert the {@link NewActivityDTO} to a {@link Activity}
     * @param newActivityDTO the DTO to convert
     * @param workId the id of the work
     * @return the converted entity
     */
    abstract public Activity toModel(NewActivityDTO newActivityDTO, String workId);
    /**
     * Convert the {@link Work} to a {@link WorkDTO}
     * @param work the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "workType", expression = "java(toWorkTypeDTOFromWorkTypeId(work.getWorkTypeId()))")
    abstract public WorkDTO toDTO(Work work);

    @Mapping(target = "activityType", expression = "java(toActivityTypeDTOFromActivityTypeId(activity.getActivityTypeId()))")
    abstract public ActivityDTO toDTO(Activity activity);

    /**
     * Convert the {@link String} work type id to a {@link WorkTypeDTO}
     * @param workTypeId the id of the work type
     * @return the converted DTO
     */
    public WorkTypeDTO toWorkTypeDTOFromWorkTypeId(String workTypeId) {
        return workTypeRepository.findById(workTypeId).map(this::toDTO).orElseThrow(
                ()-> WorkTypeNotFound.notFoundById()
                        .errorCode(-1)
                        .workId(workTypeId)
                        .build()
        );
    }

    public ActivityTypeDTO toActivityTypeDTOFromActivityTypeId(String activityTypeId) {
        return activityTypeRepository.findById(activityTypeId).map(this::toDTO).orElseThrow(
                ()-> ActivityTypeNotFound.notFoundById()
                        .errorCode(-1)
                        .activityTypeId(activityTypeId)
                        .build()
        );
    }
}
