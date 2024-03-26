package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.model.value.ValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;


@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Describe the custom field associated with the activity type. The custom fields are used to store additional information about the activity.")
public record ActivityTypeCustomFieldDTO(
        @Schema(description = "The unique identifier for the custom field.")
        String id,
        @Schema(description = "The title of the custom field. This field stores the title or name of the custom field.")
        String name,
        @Schema(description = "The user friendly label of the custom field. This field provides a user friendly label for the custom field.")
        String label,
        @Schema(description = "The detailed description of the custom field. This field provides a comprehensive description of what the custom field entails.")
        String description,
        @Schema(description = "The type of the custom field.")
        ValueTypeDTO valueType,
        @Schema(description = "Specify is the value of the custom field is a list of values.")
        Boolean isLov,
        @Schema(description = "Specify is the custom field is mandatory.")
        Boolean isMandatory) {
}
