/*
 * -----------------------------------------------------------------------------
 * Title      : WorkControllerController
 * ----------------------------------------------------------------------------
 * File       : WorkControllerController.java
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

package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/v1/work")
@Schema(description = "Set of api for the work management")
public class WorkController {
    private final WorkService workService;

    @Operation(summary = "find all works that respect the criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search operation completed successfully")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PostAuthorize("@workAuthorizationService.applyCompletionDTOList(returnObject, authentication)")
    public ApiResultResponse<List<WorkDTO>> findAllWork(
            Authentication authentication,
            @Schema(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Schema(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Schema(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Schema(name = "search", description = "Typical search functionality")
            @RequestParam(value = "search") Optional<String> search
    ) {
        return ApiResultResponse.of(
                workService.searchAllWork(
                        WorkQueryParameterDTO.builder()
                                .anchorID(anchorId.orElse(null))
                                .contextSize(contextSize.orElse(null))
                                .limit(limit.orElse(null))
                                .search(search.orElse(null))
                                .build()
                )
        );
    }
}
