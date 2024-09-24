package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import jakarta.validation.constraints.NotEmpty;

public class BaseWorkflowDomainTest {

    protected @NotEmpty String getCustomFileIdByName(WorkTypeDTO workTypeDTO, String customFieldName) {
        return workTypeDTO.customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(customFieldName)==0)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Custom field not found"))
                .id();
    }
}
