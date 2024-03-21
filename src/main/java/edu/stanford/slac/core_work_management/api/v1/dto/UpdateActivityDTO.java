package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a new activity for an existing work-plan")
public record UpdateActivityDTO(
        @Schema(description = "The title of the activity")
        String title,
        @Schema(description = "The detailed description of the activity")
        String description,
        @Schema(description = "The type of the activity")
        ActivityTypeDTO activityType,
        @Schema(description = "The subtype of the activity")
        ActivityTypeSubtypeDTO activityTypeSubtype,
        @Schema(description = "The values of the custom attributes for the activity")
        List<WriteCustomAttributeDTO> customAttributeValues
) {
}
