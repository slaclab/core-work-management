package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for update the activity type")
public record UpdateWorkTypeDTO(
        @NotEmpty(message = "The title of the activity type cannot be empty")
        @Schema(description = "The title of the activity type. This field stores the title or name of the activity type.")
        String title,
        @NotEmpty(message = "The description of the activity type cannot be empty")
        @Schema(description = "The detailed description of the activity type. This field provides a comprehensive description of what the activity type entails.")
        String description,
        @Valid @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields
) {
}