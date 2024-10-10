package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the type of work")
public record EmbeddableWorkTypeDTO(
        @Schema(description = "The unique id of the work type")
        String id,
        @Schema(description = "The id of the domain to which the work type belongs")
        String domainId,
        @Schema(description = "The title of the work type")
        String title,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<ReadWATypeCustomFieldDTO> customFields,
        @Schema(description = "The list of the work types that can be child of this one")
        Set<WorkTypeSummaryDTO> childWorkType,
        @Schema(description = "The workflow that rule the life cycle of the work that refer to this type")
        WorkflowDTO workflow,
        @Schema(description = "The name of the validator to use to validate the work")
        String validatorName
) {
}
