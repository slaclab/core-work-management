package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a new domain")
public record NewDomainDTO(
        @NotEmpty
        @Schema(description = "The domain name")
        String name,
        @NotEmpty
        @Schema(description = "The domain description")
        String description,
        @Schema(description = "The list of the workflow that can be used on this domain")
        Set<String> workflowImplementations
) {
        public NewDomainDTO {
                if(workflowImplementations == null){
                        workflowImplementations = new HashSet<>();
                }
        }
}
