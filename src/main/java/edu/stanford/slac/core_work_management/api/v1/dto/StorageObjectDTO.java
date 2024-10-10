package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.InputStream;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO for the storage object")
public record StorageObjectDTO (
        @Schema(description = "The filename of the object")
        String filename,
        @Schema(description = "The content type of the object")
        String contentType,
        @Schema(description = "The file input stream of the object")
        InputStream file
){}
