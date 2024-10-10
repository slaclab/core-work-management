package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static edu.stanford.slac.core_work_management.service.workflow.WorkflowState.*;

/**
 * This is a simple workflow for a prent work four states
 * Created -> InProgress -> ReviewToClose -> Closed
 * for testing purposes
 */
@Workflow(
        name = "TestWorkflowOne",
        description = "The workflow for a test"
)
@Component("DummyParentWorkflow")
public class DummyParentWorkflow extends BaseWorkflow {
    @Autowired
    WorkRepository workRepository;

    public DummyParentWorkflow(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    public DummyParentWorkflow() {
        validTransitions = Map.of(
                // Rule: work is created and if no children are present it can be closed by the user
                Created, Set.of(InProgress, Closed),
                // Rule: all children are closed
                InProgress, Set.of(WorkflowState.ReviewToClose),
                // Rule: admin user review and close the work
                WorkflowState.ReviewToClose, Set.of(Closed)
        );
    }

    @Override
    public boolean isCompleted(Work work) {
        if (work == null) return false;
        return work.getCurrentStatus().getStatus() == Closed;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return validTransitions.get(work.getCurrentStatus().getStatus());
    }
}
