package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A DTO that represents a new bucket slot. A bucket slot represent a preido of time where job can be submitted to be accepted or rejected.")
public record BucketDTO(
        @Schema(description = "The id of the bucket slot")
        String id,
        @Schema(description = "The description of the bucket slot")
        String description,
        @Schema(description = "The the lov value that represent the type")
        LOVValueDTO type,
        @Schema(description = "The the lov value that represent the status")
        LOVValueDTO status,
        @Schema(description = "The start date and time of the bucket slot")
        LocalDateTime from,
        @Schema(description = "The end date and time of the bucket slot")
        LocalDateTime to
) {}
