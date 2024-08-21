package edu.stanford.slac.core_work_management.api.v1.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the work type status statistics")
public record WorkTypeStatusStatisticsDTO(
        @Schema(description = "The work type")
        WorkTypeSummaryDTO workType,
        @Schema(description = "The status statistics")
        List<WorkStatusCountStatisticsDTO> status) {
}

