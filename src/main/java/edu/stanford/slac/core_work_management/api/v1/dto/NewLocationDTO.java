package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.api.v1.validator.NullOrNotEmpty;
import edu.stanford.slac.core_work_management.api.v1.validator.ValidateGroupOfFieldNotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a new activity for an work-plan")
@ValidateGroupOfFieldNotEmpty(
        fieldsToCheck = {"name", "description"},
        againstFields = {"externalLocationIdentifier"},
        message = "Not all the fields (name, description and externalLocationIdentifier) should be not null")
public record NewLocationDTO (
        @NotEmpty
        @Schema(description = "The domain where the location belongs to")
        String domainId,
        @Schema(description = "The name of the location")
        String name,
        @Schema(description = "The description of the location")
        String description,
        @Schema(description = "The external identifier for the location")
        String externalLocationIdentifier,
        @NotEmpty
        @Schema(description = "The user id that represent the location manager")
        String locationManagerUserId
){}
