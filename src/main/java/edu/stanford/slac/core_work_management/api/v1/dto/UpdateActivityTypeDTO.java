/*
 * -----------------------------------------------------------------------------
 * Title      : UpdateActivityTypeDTO
 * ----------------------------------------------------------------------------
 * File       : UpdateActivityTypeDTO.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.core_work_management.model.ActivityTypeSubtype;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for update the activity type")
public record UpdateActivityTypeDTO(
        @NotEmpty(message = "The title of the activity type cannot be empty")
        @Schema(description = "The title of the activity type. This field stores the title or name of the activity type.")
        String title,
        @NotEmpty(message = "The description of the activity type cannot be empty")
        @Schema(description = "The detailed description of the activity type. This field provides a comprehensive description of what the activity type entails.")
        String description,
        @NotEmpty @Schema(description = "The list of activity subtypes in the activity type")
        Set<ActivityTypeSubtype> activityTypeSubtypes,
        @Valid @Schema(description = "The list of the custom fields associated with the activity type. The custom fields are used to store additional information about the activity.")
        List<WATypeCustomFieldDTO> customFields
) {
}
