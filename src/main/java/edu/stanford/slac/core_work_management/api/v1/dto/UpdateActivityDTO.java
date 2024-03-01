package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
        @Schema(description = "The description of the test plan for the activity. This field provides a detailed description of the test plan associated with the activity.")
        String testPlanDescription,
        @Schema(description = "The description of the backout plan for the activity. This field provides a detailed description of the backout plan associated with the activity.")
        String backoutPlanDescription,
        @Schema(description = "The description of the system requirements for the activity. This field provides a detailed description of the system requirements associated with the activity.")
        String systemRequiredDescription,
        @Schema(description = "The description of the system effects for the activity. This field provides a detailed description of the system effects associated with the activity.")
        String systemEffectedDescription,
        @Schema(description = "The description of the risk and benefits for the activity. This field provides a detailed description of the risk and benefits associated with the activity.")
        String riskBenefitDescription,
        @Schema(description = "The description of the dependencies for the activity. This field provides a detailed description of the dependencies associated with the activity.")
        String dependenciesDescription,
        @Schema(description = "The status of the activity")
        ActivityStatusDTO activityStatus) {
}
