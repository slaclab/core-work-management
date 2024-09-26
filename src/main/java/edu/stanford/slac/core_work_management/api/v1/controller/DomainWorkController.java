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
import org.springframework.validation.annotation.Validated;
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
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ReviewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDetailsOptionDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkQueryParameterDTO;
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
@RequestMapping("/v1/domain")
@Schema(description = "Set of api for the work management")
public class DomainWorkController {
    private final WorkService workService;

    @Operation(summary = "Create a new work and return his id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work saved")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            path = "/{domainId}/work",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @workAuthorizationService.checkCanCreate(#authentication, #domainId, #newWorkDTO)")
    public ApiResultResponse<String> createNewWork(
            Authentication authentication,
            @Parameter(description = "Is the domain id to use to create the work", required = true)
            @PathVariable String domainId,
            @RequestParam(name = "logIf", required = false, defaultValue = "false")
            @Parameter(description = "Log the operation if true")
            Optional<Boolean> logIf,
            @Parameter(description = "The new work to create", required = true)
            @Validated @RequestBody NewWorkDTO newWorkDTO
    ) {
        return ApiResultResponse.of(workService.createNew(domainId, newWorkDTO, logIf));
    }

    @Operation(summary = "Update a work")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Work saved")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(
            path = "/{domainId}/work/{workId}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @workAuthorizationService.checkUpdate(#authentication, #domainId, #workId, #updateWorkDTO)")
    public ApiResultResponse<Boolean> updateWork(
            Authentication authentication,
            @Parameter(description = "Is the domain id that own the work", required = true)
            @PathVariable String domainId,
            @Parameter(description = "Is the work id to update", required = true)
            @PathVariable() String workId,
            @Valid @RequestBody UpdateWorkDTO updateWorkDTO
    ) {
        workService.update(domainId, workId, updateWorkDTO);
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
    @GetMapping(value = "/{domainId}/work/{workId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@workAuthorizationService.applyCompletionDTO(returnObject, authentication)")
    public ApiResultResponse<WorkDTO> findWorkById(
            Authentication authentication,
            @Parameter(description = "Is the id of the domain to use to find the work", required = true)
            @PathVariable String domainId,
            @Parameter(description = "Is the id of the work to find", required = true)
            @PathVariable String workId,
            @Parameter(description = "Is the flag to include the changes history")
            @RequestParam(name = "changes", required = false, defaultValue = "false") Optional<Boolean> changes,
            @Parameter(description = "Is the flag to include the model changes history")
            @RequestParam(name = "model-changes", required = false, defaultValue = "false") Optional<Boolean> modelChanges

    ) {
        return ApiResultResponse.of(
                workService.findWorkById(
                        domainId,
                        workId,
                        WorkDetailsOptionDTO.builder()
                                .changes(changes.orElse(false))
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
    @GetMapping(value = "/{domainId}/work/{workId}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<WorkDTO>> findWorkHistoryById(
            Authentication authentication,
            @Parameter(description = "Is the id of the domain that contains the work", required = true)
            @PathVariable String domainId,
            @Parameter(description = "Is the id of the work", required = true)
            @PathVariable String workId
    ) {
        return ApiResultResponse.of(
                workService.findWorkHistoryById(
                        domainId,
                        workId
                )
        );
    }
}
