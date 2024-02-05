package edu.stanford.slac.core_work_management.api.v1.dto;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record ShopGroupDTO(
        @Schema(description = "The id of the shop group")
        String id,
        @Schema(description = "The name of the shop group")
        String name,
        @Schema(description = "The description of the shop group")
        String description,
        @Schema(description = "The user ids that are part of the shop group")
        Set<PersonDTO> userEmails) {
}
