package edu.stanford.slac.core_work_management.api.v1.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;

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
        String activityTypeId,
        @Schema(description = "The subtype of the activity")
        ActivityTypeSubtypeDTO activityTypeSubtype,
        @Schema(description = "The list of the user that are assigned to the activity")
        List<String> assignedTo,
        @Schema(description = "The location of the activity, if any")
        String locationId,
        @Schema(description = "The shop group that perform the work in the location")
        String shopGroupId,
        @Schema(description = "The alternative shop group that perform the work in the location")
        String alternateShopGroupId,
        @Schema(description = "The planned start date of the activity")
        LocalDateTime plannedStartDate,
        @Schema(description = "The planned stop date of the activity")
        LocalDateTime plannedEndDate,
        @Schema(description = "The project to which the activity belongs")
        String project,
        @Valid
        @Schema(description = "The values of the custom attributes for the activity")
        List<WriteCustomFieldDTO> customFieldValues,
        @Schema(description = "The feedback comment for the activity")
        String feedbackComment
) {
        public UpdateActivityDTO {
                if (assignedTo == null) {
                        assignedTo = Collections.emptyList();
                }
                if (customFieldValues == null) {
                        customFieldValues = Collections.emptyList();
                }
        }
}
