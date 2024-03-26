/*
 * -----------------------------------------------------------------------------
 * Title      : LOVElementDTO
 * ----------------------------------------------------------------------------
 * File       : LOVElementDTO.java
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

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a lov element for a specific domain and field reference.")
public record LOVElementDTO (
    @Schema(description = "The id of the LOV element")
    String id,
    @Schema(description = "The field reference of the LOV element")
    String value,
    @Schema(description = "The human-readable label of the LOV element")
    String label,
    @Schema(description = "The description of the LOV element")
    String description
){}
