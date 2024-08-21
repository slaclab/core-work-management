package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The status of the activity.")
public enum ActivityStatusDTO {
    @Schema(description = "The activity is new")
    New,
    @Schema(description = "The activity is approved")
    Approved,
    @Schema(description = "The activity is completed")
    Completed,
    @Schema(description = "The activity is dropped")
    Drop,
    @Schema(description = "The activity is rolled")
    Roll
}
