package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatusLog;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

    /**
     * if works contains one or more children, it could be closed when
     * all the children are closed
     *
     * @param work     the work to update
     * @param newState the state to move to, this is optional and can be null
     */
    @Override
    public void update(Work work, WorkflowState newState) {
        if (work == null) return;
        // return if it is closed
        if (work.getCurrentStatus().getStatus() == WorkflowState.Closed) return;


        if (newState == null) return;
        canMoveToState(work, newState);
        // we can move to the new state
        work.setCurrentStatus(WorkStatusLog.builder().status(newState).build());
    }

    @Override
    public void canUpdate(String identityId, Work work) {

    }

    @Override
    public boolean canCreateChild(Work work) {
        return false;
    }

    @Override
    public boolean isCompleted(Work work) {
        if (work == null) return false;
        return work.getCurrentStatus().getStatus() == WorkflowState.Closed;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return validTransitions.get(work.getCurrentStatus().getStatus());
    }
}
