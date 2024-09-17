package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A DTO that represents a work type that can be admitted to a bucket slot.")
public record BucketSlotWorkTypeDTO(
        @NotEmpty(message = "The id of the domain is required")
        @Schema(description = "The unique identifier for the domain. This field is used to identify the domain to which the bucket slot belongs.")
        String domainId,
        @NotEmpty(message = "The id of the work type is required")
        @Schema(description = "The unique identifier for the work type. This field is used to identify the work type that can be admitted to the bucket slot.")
        String workTypeId
) {
}
