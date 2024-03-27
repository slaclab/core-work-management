package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

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
        String description
){}