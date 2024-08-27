package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the full list of the workflow states")
public enum WorkflowStateDTO {
    Submitted,
    PendingAssignment,
    Assigned,
    ReadyForWork,
    InProgress,
    PendingApproval,
    PendingPaperwork,
    Approved,
    WorkComplete,
    ReviewToClose,
    Closed
}
