/*
 * -----------------------------------------------------------------------------
 * Title      : LocationController
 * ----------------------------------------------------------------------------
 * File       : LocationController.java
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
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@RestController()
@RequestMapping("/v1/location")
@AllArgsConstructor
@Schema(description = "Set of api for the location management")
public class LocationController {
    AuthService authService;
    LocationService locationService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new location")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Parameter(description = "The new location to create")
            @Valid @RequestBody NewLocationDTO newLocationDTO
    ) {
        return ApiResultResponse.of(
                locationService.createNew(newLocationDTO)
        );
    }

    @GetMapping(
            path = "/{locationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find a location by id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<LocationDTO> findLocationById(
            Authentication authentication,
            @Parameter(description = "The id of the location to find")
            @PathVariable("locationId") String locationId
    ) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LocationController::findLocationById")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication)
                // should be root
        );
        return ApiResultResponse.of(
                locationService.findById(locationId)
        );
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find all locations")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LocationDTO>> findAllLocations(
            Authentication authentication,
            @Parameter(description = "The filter for the location")
            @RequestParam(value = "filter") Optional<String> filter,
            @RequestParam(value = "externalId") Optional<String> externalId
    ) {
        return ApiResultResponse.of(
                locationService.findAll(
                        LocationFilterDTO
                                .builder()
                                .text(filter.orElse(null))
                                .externalId(externalId.orElse(null))
                                .build()
                )
        );
    }
}
