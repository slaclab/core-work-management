package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketSlotDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketDTO;
import edu.stanford.slac.core_work_management.service.BucketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/v1/buckets")
@AllArgsConstructor
@Schema(description = "Set of api for bucket manipulation")
public class BucketController {
    private final BucketService bucketService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new bucket")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @bucketAuthorizationService.canCreate(#authentication,#newBucketDTO)")
    public ApiResultResponse<String> newAttachment(
            Authentication authentication,
            @RequestBody @Valid NewBucketDTO newBucketDTO
    ) throws Exception {
        return ApiResultResponse.of(bucketService.createNew(newBucketDTO));
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "find all works that respect the criteria")
    @PostAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<BucketSlotDTO>> findAll(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "contextSize", description = "Is the size of the context")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Is the limit of the query")
            @RequestParam("limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Is the search string")
            @RequestParam("search") Optional<String> search
    ) {
        return ApiResultResponse.of(
                bucketService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .anchorID(anchorId.orElse(null))
                                .contextSize(contextSize.orElse(null))
                                .limit(limit.orElse(10))
                                .search(search.orElse(null))
                                .build()
                )
        );
    }

    @GetMapping(
            value = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get a bucket by id")
    @PostAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @bucketAuthorizationService.canFindOById(#authentication,#id)")
    public ApiResultResponse<BucketSlotDTO> findById(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResultResponse.of(bucketService.findById(id));
    }

    @DeleteMapping(
            value = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a bucket by id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<Boolean> deleteById(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        bucketService.deleteById(id);
        return ApiResultResponse.of(true);
    }
}
