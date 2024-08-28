package edu.stanford.slac.core_work_management.api.v1.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateShopGroupDTO;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;

/**
 * -----------------------------------------------------------------------------
 * Title      : ShopGroup
 * ----------------------------------------------------------------------------
 * File       : ShopGroupController.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * Created    : 1/29/24
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


@RestController()
@RequestMapping("/v1/domain")
@AllArgsConstructor
@Schema(description = "Set of api for the location management")
public class ShopGroupController {
    ShopGroupService shopGroupService;

    @PostMapping(
            path="{domainId}/shop-group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Parameter(description = "The domain id")
            @PathVariable String domainId,
            @Schema(description = "The new shop group to create")
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
    @PreAuthorize("@shopGroupAuthorizationService.checkUpdate(authentication, domainId, id, updateShopGroupDTO)")
    public ApiResultResponse<Boolean> update(
            Authentication authentication,
            @Parameter(description = "The domain id")
            @PathVariable String domainId,
            @Parameter(description = "The id of the shop group to update")
            @PathVariable String id,
            @Valid @RequestBody UpdateShopGroupDTO updateShopGroupDTO
    ) {
        shopGroupService.update(domainId, id, updateShopGroupDTO);
        return ApiResultResponse.of(true);
    }

    @GetMapping(
            path = "{domainId}/shop-group",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")

    public ApiResultResponse<List<ShopGroupDTO>> findAllForDomainId(
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
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<ShopGroupDTO> findById(
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
