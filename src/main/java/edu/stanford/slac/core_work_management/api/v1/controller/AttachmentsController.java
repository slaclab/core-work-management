package edu.stanford.slac.core_work_management.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.StorageObjectDTO;
import edu.stanford.slac.core_work_management.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController()
@RequestMapping("/v1/attachment")
@AllArgsConstructor
@Schema(description = "Set of api for attachment manipulation")
public class AttachmentsController {
    AuthService authService;
    AttachmentService attachmentService;

    @PostMapping(
            consumes = {"multipart/form-data"},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new attachment")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) ")//and @attachmentAuthorizationService.canCreate(#authentication)
    public ApiResultResponse<String> newAttachment(
            Authentication authentication,
            @Schema(name = "uploadFile", description = "The file to upload", required = true)
            @RequestParam("uploadFile") MultipartFile uploadFile
    ) throws Exception {
        return ApiResultResponse.of(
                attachmentService.createAttachment(
                        StorageObjectDTO.builder()
                                .filename(uploadFile.getOriginalFilename())
                                .contentType(uploadFile.getContentType())
                                .file(uploadFile.getInputStream())
                                .build()
                        , true)
        );
    }

    @GetMapping(
            path = "/{attachmentId}/download"
            //produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @Operation(summary = "Load an attachment using an unique attachment id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)") //  and @attachmentAuthorizationService.canRead(#authentication, #attachmentId)
    public ResponseEntity<Resource> download(
            Authentication authentication,
            @Schema(name = "attachmentId", description = "The unique id of the attachment", required = true)
            @PathVariable @NotNull String attachmentId
    ) throws Exception {
        StorageObjectDTO objectDTO = attachmentService.getAttachmentContent(attachmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(objectDTO.contentType()));
        headers.setContentDisposition(
                ContentDisposition
                        .inline()
                        .filename(objectDTO.filename(), StandardCharsets.UTF_8)
                        .build()
        );
        return new ResponseEntity<>(new InputStreamResource(objectDTO.file()), headers, HttpStatus.OK);
    }

    @GetMapping(
            path = "/{attachmentId}/preview.jpg"
            //produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @Operation(summary = "Load an attachment preview using an unique attachment id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)") // and @attachmentAuthorizationService.canRead(#authentication, #attachmentId)
    public ResponseEntity<Resource> downloadPreview(
            Authentication authentication,
            @Schema(name = "attachmentId", description = "The unique id of the attachment", required = true)
            @PathVariable String attachmentId
    ) throws Exception {
        StorageObjectDTO objectDTO = attachmentService.getPreviewContent(attachmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(objectDTO.contentType()));
        headers.setContentDisposition(
                ContentDisposition
                        .inline()
                        .filename(objectDTO.filename(), StandardCharsets.UTF_8)
                        .build()
        );
        return new ResponseEntity<>(new InputStreamResource(objectDTO.file()), headers, HttpStatus.OK);
    }
}
