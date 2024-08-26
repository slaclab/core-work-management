package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Work;

import java.util.Map;
import java.util.Set;

/**
 * The workflow logic for a record type of work
 */
public class RecordWorkflow extends BaseWorkflow {
    // This map defines the valid transitions for each state
    static public Map<WorkflowState, Set<WorkflowState>> validTransitions = Map.of(
            // Rule: workStatus = "Approved"
            WorkflowState.Submitted, Set.of(WorkflowState.Approved),
            // Rule: becomes active when startDate starts
            WorkflowState.Approved, Set.of(WorkflowState.InProgress),
            // Rule: becomes active when endDate reaches or user tag completed the work
            WorkflowState.InProgress, Set.of(WorkflowState.Closed)
    );


    @Override
    public void updateWithModel(Work work) {

    }
}
