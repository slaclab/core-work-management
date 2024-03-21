package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Define the information for create new work plan
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for create new activity for work plan")
public record NewActivityTypeDTO(
        @Schema(description = "The title of the activity type")
        String title,

        @Schema(description = "The description of the activity type")
        String description,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<ActivityTypeCustomFieldDTO> customFields
){
        public NewActivityTypeDTO {
                if(customFields == null){
                        customFields = new ArrayList<>();
                }
        }
}