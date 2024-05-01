package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.service.DomainService;
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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Return a domain by his id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<DomainDTO> findDomainById(
            Authentication authentication,
            @Parameter(description = "The id of the domain to find")
            String domainId
    ) {
        return ApiResultResponse.of(
                domainService.findById(domainId)
        );
    }

    @GetMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Return all the domain")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<List<DomainDTO>> findAllDomain(
            Authentication authentication
    ) {
        return ApiResultResponse.of(
                domainService.finAll()
        );
    }
}
