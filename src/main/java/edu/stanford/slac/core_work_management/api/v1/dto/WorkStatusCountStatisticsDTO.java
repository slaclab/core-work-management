package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Work status count statistics")
public record WorkStatusCountStatisticsDTO (
        @Schema(description = "The status of the work")
        WorkStatusDTO status,
        @Schema(description = "The count of work in the status")
        Integer count
){}
