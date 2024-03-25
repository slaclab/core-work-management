package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a new activity for an existing work-plan")
public record NewActivityDTO (
        @NotEmpty
        @Schema(description = "The title of the activity")
        String title,
        @NotEmpty
        @Schema(description = "The detailed description of the activity")
        String description,
        @NotEmpty
        @Schema(description = "The type of the activity expressed by it's identifier")
        String activityTypeId,
        @NotNull
        @Schema(description = "The subtype of the activity expressed by it's identifier")
        ActivityTypeSubtypeDTO activityTypeSubtype,
        @Schema(description = "The priority of the activity")
        String schedulingProperty,
        @Schema(description = "The values of the custom attributes for the activity")
        List<WriteCustomFieldDTO> customFieldValues){
        public NewActivityDTO {
                if (customFieldValues == null) {
                        customFieldValues = Collections.emptyList();
                }
        }
}
