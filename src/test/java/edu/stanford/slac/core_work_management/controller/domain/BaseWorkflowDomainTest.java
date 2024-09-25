package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import jakarta.validation.constraints.NotEmpty;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class BaseWorkflowDomainTest {

    /**
     * Get the custom field id by name
     *
     * @param workTypeDTO    the work type dto
     * @param customFieldName the custom field name
     * @return the custom field id
     */
    protected @NotEmpty String getCustomFileIdByName(WorkTypeDTO workTypeDTO, String customFieldName) {
        return workTypeDTO.customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(customFieldName)==0)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Custom field not found"))
                .id();
    }

    /**
     * Get the work lov value id by group name and index
     *
     * @param workDTO    the work dto
     * @param fieldName  the field name
     * @param elementIndex the element index
     * @return the work lov value id by group name and index
     * @throws Exception the exception
     */
    protected @NotEmpty(message = "The value is mandatory field") String
    getWorkLovValueIdByGroupNameAndIndex(WorkDTO workDTO, String fieldName, int elementIndex) throws Exception {
        assertThat(workDTO).isNotNull();
        return workDTO.workType().customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(fieldName)==0)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Custom field not found"))
                .lovValues()
                .get(elementIndex)
                .id();
    }
}
