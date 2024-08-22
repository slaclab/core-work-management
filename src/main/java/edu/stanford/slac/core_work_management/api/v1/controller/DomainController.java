package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/v1/domain")
@AllArgsConstructor
@Schema(description = "Set of api for the domain management")
public class DomainController {
    DomainService domainService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new domain")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNewDomain(
            Authentication authentication,
            @Parameter(description = "The new location to create")
            @Valid @RequestBody NewDomainDTO newDomainDTO
    ) {
        return ApiResultResponse.of(
                domainService.createNew(newDomainDTO)
        );
    }

    @GetMapping(
            path = "/{domainId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return a domain by his id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<DomainDTO> findDomainById(
            Authentication authentication,
            @Parameter(description = "The id of the domain to find")
            @PathVariable String domainId
    ) {
        return ApiResultResponse.of(
                domainService.findById(domainId)
        );
    }

    @GetMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return all the domain")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<List<DomainDTO>> findAllDomain(
            Authentication authentication
    ) {
        return ApiResultResponse.of(
                domainService.finAll()
        );
    }

    @Operation(summary = "Return all the work types for a specific domain")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{domainId}/work-type",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<WorkTypeDTO>> findAllWorkTypes(
            Authentication authentication,
            @Parameter(description = "The domain id", required = true)
            @PathVariable @NotNull String domainId
    ) {
        return ApiResultResponse.of(domainService.findAllWorkTypes(domainId));
    }

    @Operation(summary = "Return all the activity types")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{domainId}/work-type/{workTypeId}/activity-type",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<ActivityTypeDTO>> findAllActivityTypes(
            Authentication authentication,
            @Parameter(description = "The domain id", required = true)
            @PathVariable @NotNull String domainId,
            @Parameter(description = "The work type id", required = true)
            @PathVariable @NotNull String workTypeId
    ) {
        return ApiResultResponse.of(domainService.findAllActivityTypes(domainId, workTypeId));
    }

    @Operation(summary = "Return all the activity sub types")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{domainId}/work-type/{workTypeId}/activity-type/{activityTypeId}/sub-type",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<ActivityTypeSubtypeDTO>> findAllActivitySubTypes(
            Authentication authentication,
            @Parameter(description = "The domain id", required = true)
            @PathVariable @NotNull String domainId,
            @Parameter(description = "The work type id", required = true)
            @PathVariable @NotNull String workTypeId,
            @Parameter(description = "The activity type id", required = true)
            @PathVariable @NotNull String activityTypeId
    ) {
        return ApiResultResponse.of(domainService.findAllActivitySubTypes(domainId,workTypeId,activityTypeId));
    }
}
