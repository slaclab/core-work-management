package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Workflow(
        name = "Report",
        description = "The workflow for a report"
)
@Component("ReportWorkflow")
public class ReportWorkflow extends BaseWorkflow {
    // This map defines the valid transitions for each state
    public ReportWorkflow() {
        validTransitions = Map.of(
                // Rule: AssignedTo != null
                WorkflowState.Created, Set.of(WorkflowState.PendingAssignment),
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
    }

    @Override
    public void update(Work Work, UpdateWorkflowState updateWorkflowState) {

    }

    @Override
    public boolean isValid(@NotNull NewWorkValidation newWorkValidation, ConstraintValidatorContext context) {
        List<Boolean> validationResult = new ArrayList<>();
        context.disableDefaultConstraintViolation();

        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().title(), "title", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().description(), "description", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().locationId(),"locationId", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().shopGroupId(),"shopGroupId", context));
        // Find attribute into custom attribute values
        // find subsystem custom attribute
        var subsystemAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "subsystem",
                context);
        validationResult.add(subsystemAttribute.isPresent());
        // group custom attribute
        var groupAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "group",
                context);
        validationResult.add(groupAttribute.isPresent());
        // urgency custom attribute
        var urgencyAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "urgency",
                context);
        validationResult.add(urgencyAttribute.isPresent());
        return validationResult.stream().anyMatch(b -> b);
    }

    @Override
    public boolean isValid(@NotNull UpdateWorkValidation updateWorkValidation, ConstraintValidatorContext context) {
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
        return false;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
