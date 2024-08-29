package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.workflow.BaseWorkflow;
import edu.stanford.slac.core_work_management.model.workflow.Workflow;
import edu.stanford.slac.core_work_management.model.workflow.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * This is a simple workflow with two state
 * for testing purposes
 */
@Workflow(
        name = "TestWorkflowOne",
        description = "The workflow for a test"
)
@Component("DummyWorkflow")
public class DummyWorkflow extends BaseWorkflow {
    public DummyWorkflow() {
        validTransitions = Map.of(
                // Rule: workStatus = "Approved"
                WorkflowState.Created, Set.of(WorkflowState.Closed)
        );
    }
    @Override
    public void updateWithModel(Work work) {
        // if works contains one or more children
        // it could be closed when all the children are closed
        
    }

    @Override
    public boolean isCompleted(Work work) {
        if(work==null) return false;
        return work.getCurrentStatus().getStatus()==WorkflowState.Closed;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
