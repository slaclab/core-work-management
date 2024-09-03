package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for change the work status")
public record UpdateWorkflowStateDTO(
        @Schema(description = "Try to force a new state of the work")
        @NotNull(message = "The new state of the work is mandatory")
        WorkflowStateDTO newState,
        @Schema(description = "The comment for the status change")
        String comment
) {}
