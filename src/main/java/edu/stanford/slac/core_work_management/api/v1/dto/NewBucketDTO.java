package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.api.v1.validator.ValidDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A DTO that represents a new bucket slot. A bucket slot represent a range of time where job can be submitted to be accepted or rejected.")
public record NewBucketDTO(
        @NotEmpty(message = "The list of the id of the domain is required")
        @Schema(description = "The list of the id of the domain that can use the bucket slot")
        Set<String> domainIds,
        @NotEmpty(message = "The description of the bucket slot is required")
        @Schema(description = "The description of the bucket slot")
        String description,
        @NotEmpty(message = "The id of the lov value that represent the type is required")
        @Schema(description = "The id of the lov value that represent the type")
        String type,
        @NotEmpty(message = "The id of the lov value that represent the status is required")
        @Schema(description = "The id of the lov value that represent the status")
        String status,
        @ValidDate(message = "The start date and time of the bucket slot is invalid")
        @Schema(description = "The start date and time of the bucket slot")
        LocalDateTime from,
        @ValidDate(message = "The end date and time of the bucket slot is invalid")
        @Schema(description = "The end date and time of the bucket slot")
        LocalDateTime to,
        @Schema(description = "The id of the shop group that is associated with the bucket slot")
        @NotEmpty(message = "The id of the shop group that is associated with the bucket slot is required")
        @Valid Set<BucketSlotWorkTypeDTO> admittedWorkTypeIds
) {
}
