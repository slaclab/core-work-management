package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
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

    /**
     * Convert the {@link NewWorkTypeDTO} to a {@link WorkType}
     * @param newWorkTypeDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkType toModel(NewWorkTypeDTO newWorkTypeDTO);

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
     * Convert the {@link Work} to a {@link WorkDTO}
     * @param work the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "workType", expression = "java(toWorkTypeDTOFromWorkTypeId(work.getWorkTypeId()))")
    abstract public WorkDTO toDTO(Work work);

    /**
     * Convert the {@link String} work type id to a {@link WorkTypeDTO}
     * @param workTypeId the id of the work type
     * @return the converted DTO
     */
    public WorkTypeDTO toWorkTypeDTOFromWorkTypeId(String workTypeId) {
        return workTypeRepository.findById(workTypeId).map(this::toDTO).orElseThrow(
                ()-> ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("Work type not found")
                        .errorDomain("WorkMapper")
                        .build()
        );
    }
}
