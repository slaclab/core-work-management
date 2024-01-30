package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a new activity for an work-plan")
public record NewLocationDTO (
        @Schema(description = "The unique identifier of the parent location")
        String parentId,
        @NotEmpty
        @Schema(description = "The name of the location")
        String name,
        @NotEmpty
        @Schema(description = "The description of the location")
        String description,
        @NotEmpty
        @Schema(description = "The user id that represent the location manager")
        String locationManagerUserId,
        @NotEmpty
        @Schema(description = "The shop group id that represent the location shop group")
        String locationShopGroupId
){}
