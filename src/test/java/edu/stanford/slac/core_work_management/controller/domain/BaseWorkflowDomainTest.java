package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BaseWorkflowDomainTest {

    protected @NotEmpty String getCustomFileIdByName(WorkTypeDTO workTypeDTO, String customFieldName) {
        return workTypeDTO.customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(customFieldName)==0)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Custom field not found"))
                .id();
    }

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
