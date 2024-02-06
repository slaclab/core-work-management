package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Create a new location")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Valid @RequestBody NewLocationDTO newLocationDTO
    ) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LocationController::createNew")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be root
                () -> authService.checkForRoot(authentication)
        );
        return ApiResultResponse.of(
                locationService.createNew(newLocationDTO)
        );
    }

    @GetMapping(
            path = "/{locationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Find a location by id")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<LocationDTO> findLocationById(
            Authentication authentication,
            @PathVariable("locationId") String locationId
    ) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LocationController::findLocationById")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be root
                () -> authService.checkForRoot(authentication),
                // or a reader
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        // only admin can update the domain
                        AuthorizationTypeDTO.Read,
                        // domain for all the application
                        "/cwm"
                )
        );
        return ApiResultResponse.of(
                locationService.findById(locationId)
        );
    }
}
