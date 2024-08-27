package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define a workflow")
public record WorkflowDTO (
        String id,
        String name,
        String description,
        Map<WorkflowStateDTO, Set<WorkflowStateDTO>> validTransitions
)
{}
