/*
 * -----------------------------------------------------------------------------
 * Title      : ActivityCustomAttribute
 * ----------------------------------------------------------------------------
 * File       : ActivityCustomAttribute.java
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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the custom field for the activity")
public record CustomFieldDTO(
        @Schema(description = "The unique identifier of the custom field")
        String id,
        @Schema(description = "The name of the custom field")
        String name,
        @Schema(description = "The value of the custom field")
        ValueDTO value,
        @Schema(description = "If the value.type is ValueTypeDTO.LOV type, those are the list of the possible values")
        List<LOVElementDTO> options
) {
}
