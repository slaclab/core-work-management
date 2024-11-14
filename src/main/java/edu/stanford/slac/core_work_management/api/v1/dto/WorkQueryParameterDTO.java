package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The query parameter")
public record WorkQueryParameterDTO(
        @Schema(description = "The list of domain ids to search in.")
        List<String> domainIds,
        @Schema(description = "The list of work type id to search in.")
        List<String> workTypeIds,
        @Schema(description = "Is the id to point to as starting point in the search")
        String anchorID,
        @Schema(description = "Include this number of element before the anchor")
        Integer contextSize,
        @Schema(description = "Limit the number of element after the anchor")
        Integer limit,
        @Schema(description = "Typical search functionality")
        String search,
        @Schema(description = "Filter by users that created the work")
        List<String> createdBy,
        @Schema(description = "Filter by users that are assigned to the work")
        List<String> assignedTo,
        @Schema(description = "Filter by workflow name")
        List<String> workflowName,
        @Schema(description = "Filter by workflow state")
        List<WorkflowStateDTO> workflowState,
        @Schema(description = "Filter by bucket id")
        String bucketId
        ) {}
