package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the type of work")
public record WorkTypeDTO(
        @Schema(description = "The unique id of the work type")
        @NotEmpty String id,
        @Schema(description = "The title of the work type")
        @NotEmpty String title,
        @Schema(description = "The description of when can be used ths work type")
        @NotEmpty String description
) {}
