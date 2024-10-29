/*
 * -----------------------------------------------------------------------------
 * Title      : WorkQueryParameter
 * ----------------------------------------------------------------------------
 * File       : WorkQueryParameter.java
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

import java.time.LocalDateTime;


@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A DTO that represents the query parameter for the bucket slot.")
public record BucketQueryParameterDTO(
        @Schema(description = "The id of the anchor")
        String anchorID,
        @Schema(description = "The context size")
        Integer contextSize,
        @Schema(description = "The limit of the query")
        Integer limit,
        @Schema(description = "The search string")
        String search,
        @Schema(description = "Select all the bucket slot since from to future")
        LocalDateTime from,
        @Schema(description = "Select all the bucket slot that belong to a specific domain id")
        String domainId) {}
