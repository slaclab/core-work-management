package edu.stanford.slac.core_work_management.api.v1.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateShopGroupDTO;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static java.util.Collections.emptyList;

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
@RequestMapping("/v1/shop-group")
@AllArgsConstructor
@Schema(description = "Set of api for the location management")
public class ShopGroupController {
    AuthService authService;
    ShopGroupService shopGroupService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Valid @RequestBody NewShopGroupDTO newShopGroupDTO
    ) {
        return ApiResultResponse.of(
                shopGroupService.createNew(newShopGroupDTO)
        );
    }

    @PutMapping(
            path = "/{id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Update a shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@shopGroupAuthorizationService.checkUpdate(#authentication, #id, #updateShopGroupDTO)")
    public ApiResultResponse<Boolean> update(
            Authentication authentication,
            @Parameter(description = "The id of the shop group to update")
            @PathVariable String id,
            @Valid @RequestBody UpdateShopGroupDTO updateShopGroupDTO
    ) {
        shopGroupService.update(id, updateShopGroupDTO);
        return ApiResultResponse.of(true);
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")

    public ApiResultResponse<List<ShopGroupDTO>> findAll(
            Authentication authentication
    ) {
        return ApiResultResponse.of(
                shopGroupService.findAll()
        );
    }

    @GetMapping(
            path = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new shop group")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<ShopGroupDTO> findById(
            Authentication authentication,
            @PathVariable @NotEmpty String id
    ) {
        return ApiResultResponse.of(
                shopGroupService.findById(id)
        );
    }
}
