package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for create new work plan")
public record NewWorkDTO(
        @Schema(description = "Define the type of the work to do")
        @NotEmpty(message = "Work type is required")
        String workTypeId,
        @Schema(description = "The parent work id if the work is a sub work")
        String parentWorkId,
        @Schema(description = "The title of the work plan")
        String title,
        @Schema(description = "The description of the work plan")
        String description,
        @Schema(description =
                """
                        Define the location of the work to do. Location is considered to
                        be optional for the work plan. If the location is provide it
                        shall to be matched with an inventory item frm core inventory system
                        """
        )
        String locationId,
        @Schema(description = "The shop group id that is authorized to make the works in that location")
        String shopGroupId,
        @Schema(description = "The list of the user that are assigned to the work")
        List<String> assignedTo,
        @Schema(description = "The unique identifier of the work which his is related to")
        List<String> relatedToWorkIds,
        @Valid
        @Schema(description = "The values of the custom attributes for the work")
        List<WriteCustomFieldDTO> customFieldValues
) {
    public NewWorkDTO {
        if (customFieldValues == null) {
            customFieldValues = Collections.emptyList();
        }
    }
}
