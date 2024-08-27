package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the type of work")
public record WorkTypeDTO(
        @Schema(description = "The unique id of the work type")
        String id,
        @Schema(description = "The id of the domain to which the work type belongs")
        String domainId,
        @Schema(description = "The title of the work type")
        String title,
        @Schema(description = "The description of when can be used ths work type")
        String description,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields,
        @Schema(description = "The list of the work types that can be child of this one")
        Set<String> childWorkTypeIds,
        @Schema(description = "The id of the workflow that rule the life cycle of the work that refer to this type")
        String workflowId,
        @Schema(description = "The date when the work type was created")
        LocalDateTime createdDate,
        @Schema(description = "The user that created the work type")
        String createdBy,
        @Schema(description = "The date when the work type was last modified")
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the work type")
        String lastModifiedBy
) {
}
