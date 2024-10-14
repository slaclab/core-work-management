package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * This is a simple workflow with two state
 * for testing purposes in children work
 * after the work is created it can only be closed by the user
 * Created -> Closed
 */
@Workflow(
        name = "TestWorkflowOne",
        description = "The workflow for a test"
)
@Component("DummyChildWorkflow")

public class DummyChildWorkflow extends BaseWorkflow {
    @Autowired
    WorkRepository workRepository;

    public DummyChildWorkflow(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    public DummyChildWorkflow() {
        validTransitions = Map.of(
                // Rule: has non closed children, or it has been closed
                WorkflowState.Created, Set.of(WorkflowState.Closed)
        );
    }
}
