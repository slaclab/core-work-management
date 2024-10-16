package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.HelperService;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public abstract class BaseWorkflowDomainTest {
    HelperService helperService;

    public BaseWorkflowDomainTest(HelperService helperService) {
        this.helperService = helperService;
    }

    abstract public DomainTestInfo init();
    abstract public void clean(DomainTestInfo domainTestInfo);

    /**
     * Get the custom field id by name
     *
     * @param workTypeDTO     the work type dto
     * @param customFieldName the custom field name
     * @return the custom field id
     */
    public @NotEmpty String getCustomFileIdByName(WorkTypeDTO workTypeDTO, String customFieldName) {
        return workTypeDTO.customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(customFieldName) == 0)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Custom field not found"))
                .id();
    }

    /**
     * Get the work lov value id by group name and index
     *
     * @param workDTO      the work dto
     * @param fieldName    the field name
     * @param elementIndex the element index
     * @return the work lov value id by group name and index
     * @throws Exception the exception
     */
    public @NotEmpty(message = "The value is mandatory field") String
    getWorkLovValueIdByGroupNameAndIndex(WorkDTO workDTO, String fieldName, int elementIndex) throws Exception {
        assertThat(workDTO).isNotNull();
        return workDTO.workType().customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(fieldName) == 0)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Custom field not found"))
                .lovValues()
                .get(elementIndex)
                .id();
    }

    public @NotEmpty(message = "The value is mandatory field") String
    getWorkLovValueIdByGroupNameAndIndex(WorkTypeDTO workTypeDTO, String fieldName, int elementIndex) throws Exception {
        assertThat(workTypeDTO).isNotNull();
        var lovElementDTOs = helperService.getCustomFiledLOVValue(LOVDomainTypeDTO.Work, workTypeDTO.domainId(), workTypeDTO.id(), fieldName);
        assertThat(lovElementDTOs).isNotNull();
        return lovElementDTOs.get(elementIndex).id();
    }

    public boolean checkWorkflowStatus(String domainId, String workId, WorkflowStateDTO status) {
       return helperService.checkStatusOnWork(domainId, workId, status);
    }

    public String toString(LocalDateTime date) {
        return helperService.toString(date);
    }
}
