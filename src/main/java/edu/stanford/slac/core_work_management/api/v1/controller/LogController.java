package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import edu.stanford.slac.core_work_management.service.ELogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController()
@RequestMapping("/v1/domain")
@AllArgsConstructor
@Profile("elog-support")
@Schema(description = "Set of api for the log entries management")
public class LogController {
    private final ELogService logService;

    @PostMapping(
            path = "/{domainId}/work/{workId}/log",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a log entry")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @workAuthorizationService.checkLoggingOnWork(#authentication, #domainId, #workId)")
    public ApiResultResponse<Boolean> createWorkLogEntry(
            Authentication authentication,
            @Schema(description = "The domain id")
            @PathVariable("domainId") @NotEmpty String domainId,
            @Schema(description = "The work id")
            @PathVariable("workId") @NotEmpty String workId,
            @Schema(description = "The log entry")
            @ModelAttribute @Valid NewLogEntry entry,
            @Schema(description = "The files to attach to the log entry")
            @RequestPart(value = "files", required = false)
            MultipartFile[] files
    ) {
        // create new log entry
        logService.createNewLogEntry(domainId, workId, entry, files);
        return ApiResultResponse.of(true);
    }
}
