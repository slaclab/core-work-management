package edu.stanford.slac.core_work_management.api.v1.mapper;

import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.workflow.BaseWorkflow;
import edu.stanford.slac.core_work_management.service.workflow.WorkflowState;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.StringUtility;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import org.springframework.context.ApplicationContext;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class DomainMapper {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private LOVService lovService;
    @Autowired
    private WorkTypeRepository workTypeRepository;

    /**
     * Convert a NewDomainDTO to a Domain model
     *
     * @param newDomainDTO the DTO to convert
     * @return the model
     */
    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    @Mapping(target = "workflows", source = "workflowImplementations", qualifiedByName = "toWorkflowModel")
    public abstract Domain toModel(NewDomainDTO newDomainDTO);

    /**
     * Convert a Domain model to a DTO
     *
     * @param domain the model to convert
     * @return the DTO
     */
    @Mapping(target="workflows", source="workflows", qualifiedByName="toWorkflowDTO")
    @Mapping(target = "workTypeStatusStatistics", source = "workTypeStatusStatistics", qualifiedByName = "convertStatistic")
    public abstract DomainDTO toDTO(Domain domain);

    /**
     * Convert a WorkStatusCountStatistics model to a DTO
     *
     * @param model the model to convert
     * @return the DTO
     */
    public abstract WorkStatusCountStatisticsDTO toDTO(WorkStatusCountStatistics model);

    /**
     * Convert the {@link WorkType} to a {@link WorkTypeDTO}
     *
     * @param workType the entity to convert
     * @return the converted DTO
     */
    abstract public WorkTypeDTO toDTO(WorkType workType);

    @Mapping(target = "customFields", expression = "java(updateModelCustomField(dto.customFields(), workType.getCustomFields()))")
    abstract public WorkType updateModel(UpdateWorkTypeDTO dto, @MappingTarget WorkType workType);

    /**
     * Convert a WorkType model to a WorkTypeSummaryDTO
     *
     * @param workType the model to convert
     * @return the DTO
     */
    abstract public WorkTypeSummaryDTO toSummaryDTO(WorkType workType);

    /**
     * Convert the {@link NewWorkTypeDTO} to a {@link WorkType}
     *
     * @param newWorkTypeDTO the DTO to convert
     * @param domainId       the id of the domain
     * @return the converted entity
     */
    abstract public WorkType toModel(String domainId, NewWorkTypeDTO newWorkTypeDTO);

    /**
     * Convert the {@link WATypeCustomField} to a {@link WATypeCustomFieldDTO}
     *
     * @param customField the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "isLov", expression = "java(checkIsLOV(customField))")
    abstract public WATypeCustomFieldDTO toDTO(WATypeCustomField customField);

    /**
     * Convert the {@link WATypeCustomFieldDTO} to a {@link WATypeCustomField}
     *
     * @param WATypeCustomFieldDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WATypeCustomField toModel(WATypeCustomFieldDTO WATypeCustomFieldDTO);

    /**
     * Convert the {@link WATypeCustomFieldDTO} to a {@link WATypeCustomField}
     *
     * @param id                   the id of the custom field
     * @param name                 the name of the custom field
     * @param WATypeCustomFieldDTO the DTO to convert
     * @return the converted entity
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    abstract public WATypeCustomField toModel(String id, String name, WATypeCustomFieldDTO WATypeCustomFieldDTO);

    /**
     * Convert a WorkflowStateDTO to a WorkflowState
     *
     * @param state the DTO to convert
     * @return the converted entity
     */
    abstract public UpdateWorkflowState toModel(UpdateWorkflowStateDTO state);

    /**
     * Check if the custom field is a LOV
     *
     * @param customField the custom field to check
     * @return true if the custom field is a LOV
     */
    public boolean checkIsLOV(WATypeCustomField customField) {
        if (customField.getLovFieldReference() == null) return false;
        return wrapCatch(
                () -> lovService.checkIfFieldReferenceIsInUse(customField.getLovFieldReference()),
                -1
        );
    }

    /**
     * Convert a WorkType model to a WorkTypeSummaryDTO
     *
     * @param workflowImplementations the implementations of the workflows for a new domain
     * @return the list of workflow dtos
     */
    @Named("toWorkflowModel")
    public Set<Workflow> toWorkflowModel(Set<String> workflowImplementations) {
        Set<Workflow> result = new HashSet<>();
        if (workflowImplementations == null) return result;
        workflowImplementations.forEach(
                workflowImplementation -> {
                    result.add(
                            Workflow.builder()
                                    .id(UUID.randomUUID().toString())
                                    .implementation(workflowImplementation)
                                    .build()
                    );
                }
        );
        return result;
    }

    @Named("toWorkflowDTO")
    public Set<WorkflowDTO> toWorkflowDTO(Set<Workflow> workflows) {
        Set<WorkflowDTO> result = new HashSet<>();
        if (workflows == null) return result;
        workflows.forEach(
                w -> {;
                    BaseWorkflow workFlowInstance = (BaseWorkflow) context.getBean(w.getImplementation());
                    var isPresent = workFlowInstance.getClass().isAnnotationPresent(edu.stanford.slac.core_work_management.service.workflow.Workflow.class);
                    if (!isPresent) {
                        throw ControllerLogicException.builder()
                                .errorCode(-1)
                                .errorMessage("The workflow class with name '%s' is not available".formatted(w.getImplementation()))
                                .errorDomain("DomainMapper::toWorkflowDTO")
                                .build();
                    }
                    var annot = workFlowInstance.getClass().getAnnotation(edu.stanford.slac.core_work_management.service.workflow.Workflow.class);
                    result.add(
                            WorkflowDTO.builder()
                                    .id(w.getId())
                                    .name(annot.name())
                                    .description(annot.description())
                                    .implementation(w.getImplementation())
                                    .validTransitions(toDTO(workFlowInstance.getValidTransitions()))
                                    .build()
                    );
                }
        );
        return result;
    }

    /**
     * Convert a map of WorkflowState to a map of WorkflowStateDTO
     *
     * @param validTransitions the map to convert
     * @return the converted map
     */
    private Map<WorkflowStateDTO, Set<WorkflowStateDTO>> toDTO(Map<WorkflowState, Set<WorkflowState>> validTransitions) {
        Map<WorkflowStateDTO, Set<WorkflowStateDTO>> result = new HashMap<>();
        if (validTransitions == null) return result;
        validTransitions.forEach(
                (key, value) -> {
                    result.put(
                            WorkflowStateDTO.valueOf(key.name()),
                            value.stream().map(
                                    workflowState -> WorkflowStateDTO.valueOf(workflowState.name())
                            ).collect(Collectors.toSet())
                    );
                }
        );
        return result;
    }

    /**
     * Normalize the name of the domain
     *
     * @param name the name to normalize
     * @return the normalized name
     */
    @Named("normalizeName")
    public String modifyName(String name) {
        return name.trim().toLowerCase().replace(" ", "-");
    }

    /**
     * Convert a map of WorkStatusCountStatistics to a map of WorkStatusCountStatisticsDTO
     *
     * @param value the map to convert
     * @return the converted map
     */
    @Named("convertStatistic")
    public List<WorkTypeStatusStatisticsDTO> map(Map<String, List<WorkStatusCountStatistics>> value) {
        List<WorkTypeStatusStatisticsDTO> result = new ArrayList<>();
        if (value == null) return result;
        return value.entrySet().stream()
                .map(
                        entry -> {
                            WorkType workType = workTypeRepository.findById(entry.getKey())
                                    .orElseThrow(() -> WorkTypeNotFound.notFoundById().errorCode(-1).workId(entry.getKey()).build());
                            return WorkTypeStatusStatisticsDTO
                                    .builder()
                                    .workType(toSummaryDTO(workType))
                                    .status(entry.getValue().stream().map(this::toDTO).collect(Collectors.toList()))
                                    .build();
                        }
                )
                .toList();
    }

    /**
     * Convert the {@link WATypeCustomFieldDTO} to a {@link WATypeCustomField}
     *
     * @param customFieldsDTO the lists of the new custom fields
     * @param customFields    the list of the old custom fields
     * @return the converted entity
     */
    public List<WATypeCustomField> updateModelCustomField(List<WATypeCustomFieldDTO> customFieldsDTO, List<WATypeCustomField> customFields) {
        List<WATypeCustomField> updatedCustomAttributesList = new ArrayList<>();
        customFieldsDTO.forEach(
                customFieldDTO -> {
                    // try to find the custom field in the old list
                    Optional<WATypeCustomField> customField = Objects.requireNonNullElse(customFields, Collections.<WATypeCustomField>emptyList()).stream()
                            .filter(
                                    customField1 -> customField1.getId().equals(customFieldDTO.id())
                            ).findFirst();

                    if (customField.isPresent()) {
                        updatedCustomAttributesList.add(toModel(customFieldDTO));
                    } else {
                        updatedCustomAttributesList.add(
                                toModel(
                                        UUID.randomUUID().toString(),
                                        StringUtility.toCamelCase(customFieldDTO.label()),
                                        customFieldDTO
                                )
                        );
                    }
                }
        );
        return updatedCustomAttributesList;
    }
}