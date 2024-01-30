package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for change the activity status")
public record UpdateActivityStatusDTO(
        @Valid
        @Schema(description = "The new status of the activity")
        ActivityStatusDTO newStatus,
        @NotEmpty
        @Schema(description = "The detailed description of the status change")
        String followupDescription
  )
{}
