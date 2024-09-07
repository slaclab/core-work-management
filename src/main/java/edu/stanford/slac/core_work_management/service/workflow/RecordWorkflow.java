package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.Authentication;
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
    public void update(Work work, UpdateWorkflowState updateWorkflowState) {

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
        return true;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
