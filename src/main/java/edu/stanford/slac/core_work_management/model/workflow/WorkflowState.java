package edu.stanford.slac.core_work_management.model.workflow;

/**
 * The state of the workflow
 */
public enum WorkflowState {
    Created,
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
