package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * The workflow logic for a record type of work
 */
@Workflow(
        name = "Record",
        description = "The workflow logic for a record type of work"
)
@Component("RecordWorkflow")
public class RecordWorkflow extends BaseWorkflow {

    // This map defines the valid transitions for each state
    public RecordWorkflow() {
        validTransitions = Map.of(
                // Rule: workStatus = "Approved"
                WorkflowState.Submitted, Set.of(WorkflowState.Approved),
                // Rule: becomes active when startDate starts
                WorkflowState.Approved, Set.of(WorkflowState.InProgress),
                // Rule: becomes active when endDate reaches or user tag completed the work
                WorkflowState.InProgress, Set.of(WorkflowState.Closed)
        );
    }

    @Override
    public void updateWithModel(Work work) {

    }

    @Override
    public boolean isCompleted(Work work) {
        return true;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
