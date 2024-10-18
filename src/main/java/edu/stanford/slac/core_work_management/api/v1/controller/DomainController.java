package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LocationService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/v1/domain")
@AllArgsConstructor
@Schema(description = "Set of api for the domain management")
public class DomainController {
    DomainService domainService;
    LocationService locationService;
    ShopGroupService shopGroupService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new domain")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNewDomain(
            Authentication authentication,
            @Schema(description = "The new domain to create", implementation = NewDomainDTO.class)
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
            @Schema(description = "The id of the domain to find")
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
            @Schema(description = "The domain id", required = true)
            @PathVariable @NotNull String domainId
    ) {
        return ApiResultResponse.of(domainService.findAllWorkTypes(domainId));
    }


    @PostMapping(
            path = "/{domainId}/location",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new root location")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNewRootLocation(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable @NotEmpty String domainId,
            @Schema(description = "The new location to create", implementation = NewLocationDTO.class)
            @Valid @RequestBody NewLocationDTO newLocationDTO
    ) {
        return ApiResultResponse.of(
                locationService.createNew(domainId, newLocationDTO)
        );
    }

    @PostMapping(
            path = "/{domainId}/location/{locationId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new child location")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNewChildLocation(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable @NotEmpty String domainId,
            @Schema(description = "The id of the parent location")
            @PathVariable @NotEmpty String locationId,
            @Schema(description = "The new location to create", implementation = NewLocationDTO.class)
            @Valid @RequestBody NewLocationDTO newLocationDTO
    ) {
        return ApiResultResponse.of(
                locationService.createNewChild(domainId, locationId, newLocationDTO)
        );
    }

    @GetMapping(
            path = "/{domainId}/location/{locationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find a location by id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<LocationDTO> findLocationById(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable @NotNull String domainId,
            @Schema(description = "The id of the location to find")
            @PathVariable("locationId") String locationId
    ) {
        return ApiResultResponse.of(
                locationService.findById(domainId, locationId)
        );
    }

    @GetMapping(
            path = "/{domainId}/location",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find all locations")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LocationDTO>> findAllLocations(
            Authentication authentication,
            @Schema(description = "The domain id", required = true)
            @PathVariable @NotNull String domainId,
            @Schema(description = "The filter for the location")
            @RequestParam(value = "filter") Optional<String> filter,
            @Schema(description = "The external id of the location")
            @RequestParam(value = "externalId") Optional<String> externalId
    ) {
        return ApiResultResponse.of(
                locationService.findAll(
                        domainId,
                        LocationFilterDTO
                                .builder()
                                .text(filter.orElse(null))
                                .externalId(externalId.orElse(null))
                                .build()
                )
        );
    }


    @PostMapping(
            path="{domainId}/shop-group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNewShopGroup(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable String domainId,
            @Schema(description = "The new shop group to create", implementation = NewShopGroupDTO.class)
            @Valid @RequestBody NewShopGroupDTO newShopGroupDTO
    ) {
        return ApiResultResponse.of(
                shopGroupService.createNew(domainId, newShopGroupDTO)
        );
    }

    @PutMapping(
            path = "{domainId}/shop-group/{id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Update a shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @shopGroupAuthorizationService.checkUpdate(#authentication, #domainId, #id, #updateShopGroupDTO)")
    public ApiResultResponse<Boolean> updateShopGroup(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable String domainId,
            @Schema(description = "The id of the shop group to update")
            @PathVariable String id,
            @Schema(description = "The new shop group to update", implementation = UpdateShopGroupDTO.class)
            @Valid @RequestBody UpdateShopGroupDTO updateShopGroupDTO
    ) {
        shopGroupService.update(domainId, id, updateShopGroupDTO);
        return ApiResultResponse.of(true);
    }

    @GetMapping(
            path = "{domainId}/shop-group",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Find all the  shop group for a domain")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")

    public ApiResultResponse<List<ShopGroupDTO>> findAllShopGroupForDomain(
            Authentication authentication,
            @PathVariable @NotEmpty String domainId
    ) {
        return ApiResultResponse.of(
                shopGroupService.findAllByDomainId(domainId)
        );
    }

    @GetMapping(
            path = "{domainId}/shop-group/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Get a full shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<ShopGroupDTO> findShopGroupByDomainAndId(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable @NotEmpty String domainId,
            @Schema(description = "The shop group id")
            @PathVariable @NotEmpty String id
    ) {
        return ApiResultResponse.of(
                shopGroupService.findByDomainIdAndId(domainId, id)
        );
    }
}
