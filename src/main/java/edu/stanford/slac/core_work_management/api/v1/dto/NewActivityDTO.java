package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.model.ActivityStatus;
import edu.stanford.slac.core_work_management.model.ActivityTypeSubtype;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

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
        ActivityTypeSubtypeDTO activityTypeSubtype){}
