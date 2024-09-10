package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.exception.WorkflowNotManuallyUpdatable;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatusLog;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static edu.stanford.slac.core_work_management.service.workflow.WorkflowState.Created;

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
     * @param work                the work to update
     * @param updateWorkflowState the state to move to, this is optional and can be null
     */
    @Override
    public void update(Work work, WorkType workType, UpdateWorkflowState updateWorkflowState) {
        if (work == null) return;
        if (updateWorkflowState != null && work.getCurrentStatus().getStatus() != Created) {
            throw WorkflowNotManuallyUpdatable.of()
                    .errorCode(-1)
                    .build();
        }
        // return if it is closed
        if (work.getCurrentStatus().getStatus() == WorkflowState.Closed) return;

        if (updateWorkflowState == null) return;
        moveToState(work, updateWorkflowState);
    }

    @Override
    public boolean isValid(NewWorkValidation newWorkValidation, ConstraintValidatorContext context) {
        return true;
    }

    @Override
    public boolean isValid(UpdateWorkValidation updateWorkValidation, ConstraintValidatorContext context) {
        return true;
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
