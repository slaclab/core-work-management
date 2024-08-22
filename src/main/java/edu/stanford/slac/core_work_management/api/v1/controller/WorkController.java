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

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityStatusDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivitySummaryDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityTypeSubtypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ReviewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityStatusDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDetailsOptionDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

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
    public ApiResultResponse<String> createNewWork(
            Authentication authentication,
            @RequestParam(name = "logIf", required = false, defaultValue = "false")
            @Parameter(description = "Log the operation if true")
            Optional<Boolean> logIf,
            @Parameter(description = "The new work to create", required = true)
            @Valid @RequestBody NewWorkDTO newWorkDTO
    ) {
        return ApiResultResponse.of(workService.createNew(newWorkDTO, logIf));
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
    public ApiResultResponse<Boolean> updateWork(
            Authentication authentication,
            @Parameter(description = "Is the work id to update", required = true)
            @PathVariable() String workId,
            @Valid @RequestBody UpdateWorkDTO updateWorkDTO
    ) {
        workService.update(workId, updateWorkDTO);
        return ApiResultResponse.of(true);
    }

    @Operation(summary = "Review a work")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity updated")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(
            path = "/{workId}/review",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkReviewWork(#authentication, #workId,#reviewWorkDTO)")
    public ApiResultResponse<Boolean> reviewWork(
            Authentication authentication,
            @Parameter(description = "Is the work id that contains the activity", required = true)
            @PathVariable String workId,
            @Valid @RequestBody ReviewWorkDTO reviewWorkDTO
    ) {
        workService.reviewWork(workId, reviewWorkDTO);
        return ApiResultResponse.of(true);
    }

    @Operation(
            summary = "Get full work by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The work found"),
                    @ApiResponse(responseCode = "404", description = "Work not found")
            }
    )
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/{workId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@workAuthorizationService.applyCompletionDTO(returnObject, authentication)")
    public ApiResultResponse<WorkDTO> findWorkById(
            Authentication authentication,
            @Parameter(description = "Is the id of the work to find", required = true)
            @PathVariable String workId,
            @Parameter(description = "Is the flag to include the changes history")
            @RequestParam(name = "changes", required = false, defaultValue = "false") Optional<Boolean> changes,
            @Parameter(description = "Is the flag to include the model changes history")
            @RequestParam(name = "model-changes", required = false, defaultValue = "false") Optional<Boolean> modelChanges

    ) {
        return ApiResultResponse.of(
                workService.findWorkById(
                        workId,
                        WorkDetailsOptionDTO.builder()
                                .changes(changes)
                                .build()
                )
        );
    }

    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get work history by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of the found history state of the work")
            }
    )
    @GetMapping(value = "/{workId}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<WorkDTO>> findWorkHistoryById(
            Authentication authentication,
            @Parameter(description = "Is the id of the work to use to find the history", required = true)
            @PathVariable String workId
    ) {
        return ApiResultResponse.of(
                workService.findWorkHistoryById(
                        workId
                )
        );
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
    public ApiResultResponse<String> createNewActivity(
            Authentication authentication,
            @Parameter(description = "Is the work id for wich needs to be creates the activity", required = true)
            @PathVariable("workId") String workId,
            @Parameter(description = "The new activity to create", required = true)
            @Valid @RequestBody NewActivityDTO newActivityDTO) {
        return ApiResultResponse.of(workService.createNew(workId, newActivityDTO));
    }

    @Operation(summary = "Return all the activity that belongs to a work")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{workId}/activity",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<ActivitySummaryDTO>> findAllActivityByWorkId(
            Authentication authentication,
            @Parameter(description = "Is the work id", required = true)
            @PathVariable("workId") String workId) {
        return ApiResultResponse.of(workService.findAllActivitiesByWorkId(workId));
    }

    @Operation(summary = "Update an activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity updated")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(
            path = "/{workId}/activity/{activityId}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkUpdate(#authentication, #workId,#activityId, #updateActivityDTO)")
    public ApiResultResponse<Boolean> updateActivity(
            Authentication authentication,
            @Parameter(description = "Is the work id that contains the activity", required = true)
            @PathVariable String workId,
            @Parameter(description = "Is the activity id to update", required = true)
            @PathVariable String activityId,
            @Valid @RequestBody UpdateActivityDTO updateActivityDTO
    ) {
        workService.update(workId, activityId, updateActivityDTO);
        return ApiResultResponse.of(true);
    }


    @Operation(summary = "Update an activity status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity updated")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(
            path = "/{workId}/activity/{activityId}/status",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@workAuthorizationService.checkUpdateStatus(#authentication, #workId,#activityId, #updateActivityStatusDTO)")
    public ApiResultResponse<Boolean> setActivityStatus(
            Authentication authentication,
            @Parameter(description = "Is the work id that contains the activity", required = true)
            @PathVariable String workId,
            @Parameter(description = "Is the activity id to update", required = true)
            @PathVariable String activityId,
            @Valid @RequestBody UpdateActivityStatusDTO updateActivityStatusDTO
    ) {
        workService.setActivityStatus(workId, activityId, updateActivityStatusDTO);
        return ApiResultResponse.of(true);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/activity/status/{status}/permitted",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Return the list of possible status for the activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity updated")
    })
    @PreAuthorize("@workAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<ActivityStatusDTO>> getPermittedStatus(
            Authentication authentication,
            @Parameter(description = "Is the activity status to test", required = true)
            @PathVariable ActivityStatusDTO status
    ) {
        return ApiResultResponse.of(workService.getPermittedStatus(status));
    }


    @Operation(summary = "Get work by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity found"),
            @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/{workId}/activity/{activityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<ActivityDTO> findActivityById(
            Authentication authentication,
            @Parameter(description = "Is the id of the work to find", required = true)
            @PathVariable String workId,
            @Parameter(description = "Is the id of the activity to find", required = true)
            @PathVariable String activityId
    ) {
        return ApiResultResponse.of(workService.findActivityById(activityId));
    }

    @Operation(summary = "find all works that respect the criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search operation completed successfully")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PostAuthorize("@workAuthorizationService.applyCompletionDTOList(returnObject, authentication)")
    public ApiResultResponse<List<WorkDTO>> findAllWork(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Typical search functionality")
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

    @Operation(summary = "find all activities that respect the criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search operation completed successfully")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/activity",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<ActivityDTO>> findAllActivities(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Typical search functionality")
            @RequestParam(value = "search") Optional<String> search
    ) {
        return ApiResultResponse.of(
                workService.searchAllActivities(
                        ActivityQueryParameterDTO.builder()
                                .anchorID(anchorId.orElse(null))
                                .contextSize(contextSize.orElse(null))
                                .limit(limit.orElse(null))
                                .search(search.orElse(null))
                                .build()
                )
        );
    }
}
