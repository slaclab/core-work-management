package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Define the information for create new work plan
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for create new work plan")
public record NewWorkTypeDTO (
        @NotEmpty(message = "The title of the work type cannot be empty")
        @Schema(description = "The title of the work type")
        String title,
        @NotEmpty(message = "The description of the work type cannot be empty")
        @Schema(description = "The description of the work type")
        String description,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields,
        @Schema(description = "The list of the work types that can be child of this one")
        Set<String> childWorkTypeIds,
        @NotNull(message = "The id of the workflow cannot be null")
        @Schema(description = "The id of the workflow that rule the life cycle of the work that refer to this type")
        String workflowId
){
        public NewWorkTypeDTO {
                if(customFields == null){
                        customFields = new ArrayList<>();
                }
                if (childWorkTypeIds == null){
                        childWorkTypeIds = Set.of();
                }
        }
}