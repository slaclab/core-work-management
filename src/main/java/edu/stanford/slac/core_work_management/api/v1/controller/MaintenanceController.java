package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketDTO;
import edu.stanford.slac.core_work_management.model.BucketSlotQueryParameter;
import edu.stanford.slac.core_work_management.service.BucketService;
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
@RequestMapping("/v1/maintenance")
@AllArgsConstructor
@Schema(description = "Set of api for the maintenance management")
public class MaintenanceController {
    private final BucketService bucketService;

    @PostMapping(
            path = "/bucket",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new bucket slot", description = "This method is used to create a new bucket slot")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createNew(
            Authentication authentication,
            @Valid @RequestBody NewBucketDTO newBucketSlotDTO
    ) {
        return ApiResultResponse.of(
                bucketService.createNew(newBucketSlotDTO)
        );
    }

    @GetMapping(
            path = "/bucket/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find a bucket slot by id", description = "This method is used to find a bucket slot by id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<BucketDTO> findById(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResultResponse.of(
                bucketService.findById(id)
        );
    }

    @GetMapping(
            path = "/bucket",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Find all bucket slots", description = "This method is used to find all bucket slots")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<List<BucketDTO>> findAll
            (
                    Authentication authentication,
                    @Parameter(description = "The maximum number of bucket slots to return")
                    @RequestParam(value = "limit", required = false, defaultValue = "0") int limit,
                    @Parameter(description = "The size of the context to return")
                    @RequestParam(value = "contextSize", required = false, defaultValue = "0") int contextSize,
                    @Parameter(description = "The id of the anchor to use for pagination")
                    @RequestParam(value = "anchorId", required = false) String anchorId

            ) {
        return ApiResultResponse.of(
                bucketService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .limit(limit)
                                .contextSize(contextSize)
                                .anchorID(anchorId)
                                .build()
                )
        );
    }
}
