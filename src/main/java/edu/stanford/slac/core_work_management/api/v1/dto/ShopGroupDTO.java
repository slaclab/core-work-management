package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the full readable information for shop group")
public record ShopGroupDTO(
        @Schema(description = "The id of the shop group")
        String id,
        @Schema(description = "The domain where the shop group belongs to")
        DomainDTO domain,
        @Schema(description = "The name of the shop group")
        String name,
        @Schema(description = "The description of the shop group")
        String description,
        @Schema(description = "The user ids that are part of the shop group")
        Set<ShopGroupUserDTO> users,
        @Schema(description = "The created date of the work")
        LocalDateTime createdDate,
        @Schema(description = "The user that created the work")
        String createdBy,
        @Schema(description = "The last modified date of the work")
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the work")
        String lastModifiedBy) {
}
