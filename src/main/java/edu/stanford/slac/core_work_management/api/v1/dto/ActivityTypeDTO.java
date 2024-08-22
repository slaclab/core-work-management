package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.model.ActivityTypeSubtype;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information the activity of work")
public record ActivityTypeDTO(
        @Schema(description = "The unique id of the activity type")
        String id,
        @Schema(description = "The unique id of the domain to which the activity type belongs")
        String domainId,
        @Schema(description = "The unique id of the work type to which the activity type belongs")
        String workTypeId,
        @Schema(description = "The title of the activity type")
        String title,
        @Schema(description = "The description of when can be used ths activity type")
        String description,
        @Schema(description = "The list of activity subtypes in the activity type")
        Set<ActivityTypeSubtypeDTO> activityTypeSubtypes,
        @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields
) {
}
