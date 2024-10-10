/*
 * -----------------------------------------------------------------------------
 * Title      : UpdateWorkDTO
 * ----------------------------------------------------------------------------
 * File       : UpdateWorkDTO.java
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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.stanford.slac.core_work_management.api.v1.validator.NullOrNotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for change the activity status")
public record UpdateWorkDTO(
        @Schema(description = "The work id")
        String relatedToWorkId,
        @Schema(description = "The new title")
        String title,
        @Schema(description = "The new description")
        String description,
        @Schema(description = "The list of the user that are assigned to the work")
        List<String> assignedTo,
        @Schema(description =
                """
                Define the location of the work to do
                """
        )
        @NullOrNotEmpty(message = "Location id can be null or not empty")
        String locationId,
        @NullOrNotEmpty(message = "Shop group can be null or not empty")
        @Schema(description = "The shop group id that is authorized to make the works in that location")
        String shopGroupId,
        @Schema(description = "The unique identifier of the work which his is related to")
        List<String> relatedToWorkIds,
        @Schema(description = "Force to change the workflow state(it it will be checked if permitted)")
        UpdateWorkflowStateDTO workflowStateUpdate,
        @Valid
        @Schema(description = "The values of the custom attributes for the activity")
        List<WriteCustomFieldDTO> customFieldValues,
        @Valid
        @Schema(description = "The list of the attachment id to associate to the work")
        List<String> attachments
){}
