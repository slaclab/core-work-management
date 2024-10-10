package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;


@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Describe the custom field associated with the work type. The custom fields are used to store additional information about the work.")
public record ReadWATypeCustomFieldDTO(
        @Schema(description = "The unique identifier for the custom field.")
        String id,
        @Schema(description = "The user friendly label of the custom field. This field provides a user friendly naming for the custom field.")
        String name,
        @Schema(description = "The title of the custom field. This field stores the title or name of the custom field.")
        String label,
        @Schema(description = "The group of the custom field. This field provides a group for the custom field. is needed only to help uis to group custom fields together.")
        String group,
        @Schema(description = "The detailed description of the custom field. This field provides a comprehensive description of what the custom field entails.")
        String description,
        @Schema(description = "The type of the custom field.")
        ValueTypeDTO valueType,
        @Schema(description = "The possible value of the custom field, in case it is of type LOV")
        List<LOVElementDTO> lovValues,
        @Schema(description = "Specify is the custom field is mandatory.")
        Boolean isMandatory) {
        public ReadWATypeCustomFieldDTO {
                if(isMandatory == null) {
                        isMandatory = false;
                }
        }
}
