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
@Schema(description = "Define the information for create new work plan")
public record NewWorkDTO (
        @Schema(description = "The title of the work plan")
        @NotEmpty(message = "Title is required")
        String title,

        @Schema(description = "The description of the work plan")
        @NotEmpty(message = "Description is required")
        String description,

        @Schema(description = "Define the type of the work to do")
        @NotEmpty(message = "Work type is required")
        String workTypeId,

        @Schema(description =
                """
                Define the location of the work to do. Location is considered to
                be optional for the work plan. If the location is provide it
                shall to be matched with an inventory item frm core inventory system
                """
        )
        @NotEmpty(message = "Location is required")
        String locationId,
        @NotEmpty(message = "Shop group is mandatory is required")
        @Schema(description = "The shop group id that is authorized to make the works in that location")
        String shopGroupId
){}
