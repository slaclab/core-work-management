package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.Work;

import java.util.Map;
import java.util.Set;

public class ReportWorkflow extends BaseWorkflow {
    // This map defines the valid transitions for each state
    static public Map<WorkflowState, Set<WorkflowState>> validTransitions = Map.of(
            // Rule: AssignedTo != null
            WorkflowState.Submitted, Set.of(WorkflowState.PendingAssignment),
            // Rule: if one or more safety forms were required, those forms must be attached. if no safety forms are required, move on to next state
            WorkflowState.PendingAssignment, Set.of(WorkflowState.PendingPaperwork, WorkflowState.PendingApproval),
            // Rule: when admin changes field of workStatus = "Approve"
            WorkflowState.PendingApproval, Set.of(WorkflowState.ReadyForWork),
            // Rule: all required paperwork attached and admin set directly the status to "Approved"
            WorkflowState.ReadyForWork, Set.of(WorkflowState.Approved),
            // Rule: when the start date is reached or user automatically set the state in progress
            WorkflowState.Approved, Set.of(WorkflowState.InProgress),
            // Rule: In Progress state becomes active based on start date and time of scheduled activity
            WorkflowState.InProgress, Set.of(WorkflowState.WorkComplete),
            // Rule: user/admin manually se the work to closed
            WorkflowState.WorkComplete, Set.of(WorkflowState.ReviewToClose),
            // Rule: When all activities have ended admin can close the work
            WorkflowState.ReviewToClose, Set.of(WorkflowState.Closed)
    );

    @Override
    public void updateWithModel(Work work, Set<Activity> activities) {

    }
}
