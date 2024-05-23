package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the type of work")
public record WorkTypeSummaryDTO(
        @Schema(description = "The unique id of the work type")
        @NotEmpty String id,
        @Schema(description = "The title of the work type")
        @NotEmpty String title,
        @Schema(description = "The date when the work type was created")
        LocalDateTime createdDate,
        @Schema(description = "The user that created the work type")
        String createdBy,
        @Schema(description = "The date when the work type was last modified")
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the work type")
        String lastModifiedBy
) {
}
