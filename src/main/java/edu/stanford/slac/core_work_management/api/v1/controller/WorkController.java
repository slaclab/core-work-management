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
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
import edu.stanford.slac.core_work_management.service.authorization.WorkAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * -----------------------------------------------------------------------------
 * Title      : WorkController
 * ----------------------------------------------------------------------------
 * File       : WorkControllerController.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * Created    : 2/26/24
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 * contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 **/

@AllArgsConstructor
@RestController
@RequestMapping("/v1/work")
@Schema(description = "Set of api for the work management")
public class WorkController {
    private final WorkService workService;

    @Operation(summary = "Create a new work and return his id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work saved")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Valid @RequestBody NewWorkDTO newWorkDTO
    ) {
        return ApiResultResponse.of(workService.createNew(newWorkDTO));
    }

    @Operation(summary = "Update a work")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work saved")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(
            path = "/{workId}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkUpdate(#authentication, #workId, #updateWorkDTO)")
    public ApiResultResponse<Boolean> update(
            Authentication authentication,
            @Parameter(description = "Is the work id to update", required = true)
            @PathVariable() String workId,
            @Valid @RequestBody UpdateWorkDTO updateWorkDTO
    ) {
        workService.update(workId, updateWorkDTO);
        return ApiResultResponse.of(true);
    }

    @Operation(summary = "Get work by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Work found"),
            @ApiResponse(responseCode = "404", description = "Work not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/{workId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<WorkDTO> findById(
            Authentication authentication,
            @Parameter(description = "Is the id of the work to find", required = true)
            @PathVariable String workId
    ) {
        return ApiResultResponse.of(workService.findWorkById(workId));
    }

    @Operation(summary = "Create a new work activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work saved")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            path = "/{workId}/activity",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkCreateNewActivity(#authentication, #workId)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Parameter(description = "Is the work id for wich needs to be creates the activity", required = true)
            @PathVariable("workId") String workId,
            @Parameter(description = "The new activity to create", required = true)
            @Valid @RequestBody NewActivityDTO newActivityDTO) {
        return ApiResultResponse.of(workService.createNew(workId, newActivityDTO));
    }

    @Operation(summary = "find all element that respect the criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search operation completed successfully")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ApiResultResponse<List<WorkDTO>> findAllElements(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Typical search functionality")
            @RequestParam("search") Optional<String> search,
            @Parameter(name = "tags", description = "Only include entries that use one of these tags")
            @RequestParam("tags") Optional<List<String>> tags,
            @Parameter(name = "requireAllTags", description = "Require that all entries found includes all the tags")
            @RequestParam(value = "requireAllTags", defaultValue = "false") Optional<Boolean> requireAllTags
    ) {
        return ApiResultResponse.of(
                Collections.emptyList()
        );
    }
}
