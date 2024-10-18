/*
 * -----------------------------------------------------------------------------
 * Title      : LOVController
 * ----------------------------------------------------------------------------
 * File       : LOVController.java
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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.service.LOVService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;

@RestController()
@RequestMapping("/v1/lov")
@AllArgsConstructor
@Schema(description = "Set of api for the LOV management")
public class LOVController {
    LOVService lovService;

    @Operation(summary = "Return all the lov values for a work field. for the Bucket domain uses 'bucket' as subtype")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{domainType}/{domainId}/{subtypeId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<String>> findAllFieldThatAreLOV(
            Authentication authentication,
            @Schema(description = "The domain type")
            @NotEmpty @PathVariable LOVDomainTypeDTO domainType,
            @Schema(description = "The domain id")
            @NotEmpty @PathVariable String domainId,
            @Schema(description = "The subtype id")
            @NotEmpty @PathVariable String subtypeId
    ) {
        if(domainType==LOVDomainTypeDTO.Bucket && !subtypeId.equalsIgnoreCase("bucket")) {
            throw ControllerLogicException.builder()
                    .errorMessage("Invalid subtype for Bucket domain, it should be 'bucket'")
                    .errorCode(-1)
                    .errorDomain("LOVController::findAllFieldThatAreLOV")
                    .build();
        }
        return ApiResultResponse.of(lovService.findAllLOVField(domainType, domainId, subtypeId));
    }

    @Operation(summary = "Return all the lov values for a work field.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(
            path = "/{domainType}/{domainId}/{subtypeId}/{fieldName}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LOVElementDTO>> findValuesByDomainAndFieldName(
            Authentication authentication,
            @Schema(description = "The domain type")
            @PathVariable LOVDomainTypeDTO domainType,
            @Schema(description = "The domain id")
            @NotEmpty @PathVariable String domainId,
            @Schema(description = "The subtype id")
            @NotEmpty @PathVariable String subtypeId,
            @Schema(description = "The field name")
            @NotEmpty @PathVariable String fieldName
    ) {
        return ApiResultResponse.of(lovService.findAllByDomainAndFieldName(domainType, domainId, subtypeId, fieldName));
    }
}
