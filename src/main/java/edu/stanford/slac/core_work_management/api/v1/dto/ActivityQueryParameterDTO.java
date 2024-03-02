package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The query parameter")
public record ActivityQueryParameterDTO(
        @Schema(description = "Is the id to point to as starting point in the search")
        String anchorID,
        @Schema(description = "Include this number of element before the anchor")
        Integer contextSize,
        @Schema(description = "Limit the number of element after the anchor.")
        Integer limit,
        @Schema(description = "Typical search functionality.")
        String search
        ) {}
