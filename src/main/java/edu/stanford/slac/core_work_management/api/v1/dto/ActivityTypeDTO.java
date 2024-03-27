package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the activity of work")
public record ActivityTypeDTO(
        @Schema(description = "The unique id of the activity type")
        @NotEmpty String id,
        @Schema(description = "The title of the activity type")
        @NotEmpty String title,
        @Schema(description = "The description of when can be used ths activity type")
        @NotEmpty String description,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields
) {
}
