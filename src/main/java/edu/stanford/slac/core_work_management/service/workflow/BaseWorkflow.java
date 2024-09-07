package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WriteCustomFieldDTO;
import edu.stanford.slac.core_work_management.model.*;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import lombok.Data;

import java.util.*;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

/**
 * Base class for all workflows
 */
@Data
public abstract class BaseWorkflow {
    /**
     * The valid transitions for each state
     */
    protected Map<WorkflowState, Set<WorkflowState>> validTransitions;

    /**
     * Update the workflow with the model for automatic transition
     * if a new state is provided it try to understand if the transition is valid
     * and in case new state is set
     *
     * @param work        the work to update
     * @param updateState the state to move to, this is optional and can be null
     * @throws ControllerLogicException if the transition is not valid
     */
    abstract public void update(Work work, UpdateWorkflowState updateState);

    /**
     * Check if the work is valid
     * the validation is done according to the state of the workflow
     *
     * @param newWorkValidation the work validation info to check
     */
    abstract public boolean isValid(NewWorkValidation newWorkValidation, ConstraintValidatorContext context);

    /**
     * Check if the work update are valid
     * the validation is done according to the state of the workflow
     *
     * @param updateWorkValidation the work update information to check
     */
    abstract public boolean isValid(UpdateWorkValidation updateWorkValidation, ConstraintValidatorContext context);

    /**
     * Check if the work can have children
     *
     * @param work the work to create a child for
     */
    abstract public boolean canCreateChild(Work work);

    /**
     * Check if the work is completed
     *
     * @param work the work to check
     */
    abstract public boolean isCompleted(Work work);

    /**
     * Return the permitted status for the work
     *
     * @param work the work to check
     */
    abstract public Set<WorkflowState> permittedStatus(Work work);

    /**
     * Check if the work can move to the state
     *
     * @param work     the work to check
     * @param newState the state to move to
     */
    protected void canMoveToState(Work work, UpdateWorkflowState newState) {
        if (newState == null) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("Cannot move to null state")
                    .errorDomain("BaseWorkflow::update")
                    .build();
        }
        assertion(
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("Cannot move to state %s from %s".formatted(newState.getNewState().name(), work.getCurrentStatus().getStatus().name()))
                        .errorDomain("BaseWorkflow::update")
                        .build(),
                () -> validTransitions != null && validTransitions.get(work.getCurrentStatus().getStatus()).contains(Objects.requireNonNullElse(newState.getNewState(), WorkflowState.None))
        );
    }

    /**
     * Check if the work can move to the state
     *
     * @param work     the work to check
     * @param newState the state to move to
     */
    protected void moveToState(Work work, UpdateWorkflowState newState) {
        canMoveToState(work, newState);
        // add current status to the history
        work.getStatusHistory().addFirst(work.getCurrentStatus());
        // we can move to the new state
        work.setCurrentStatus(WorkStatusLog
                .builder()
                .status(newState.getNewState())
                .comment(newState.getComment())
                .build());
    }

    /**
     * Check if the user can update the work
     *
     * @param identityId the user that is trying to update the work
     * @param work       the work
     * @throws ControllerLogicException if the user cannot update the work
     */
    public void canUpdate(String identityId, Work work) {
        if (isCompleted(work)) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("Cannot update a completed work")
                    .errorDomain("DummyParentWorkflow::canUpdate")
                    .build();
        }
    }

    /**
     * Check if the status of the work is equal to the provided state
     *
     * @param work  the work to check
     * @param state the state to check
     * @return true if the status is equal to the provided state
     */
    protected boolean isStatusEqualTo(Work work, WorkflowState state) {
        if (work == null) return false;
        var currentStatus = work.getCurrentStatus().getStatus();
        return currentStatus == state;
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param work  the work to check
     * @param state the states to check
     * @return true if the status is equal to any of the provided states
     */
    protected boolean isStatusEqualTo(Work work, Set<WorkflowState> state) {
        if (work == null) return false;
        var currentStatus = work.getCurrentStatus().getStatus();
        return state.stream().anyMatch(s -> s == currentStatus);
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param customFields  the lis tof the custom field associated to the work type
     * @param writeCustomFieldList the user valorized custom field
     * @param customFieldName the name of the custom field to check
     */
    protected Optional<WriteCustomFieldDTO> checkFiledPresence(List<WATypeCustomField> customFields, List<WriteCustomFieldDTO> writeCustomFieldList, String customFieldName, ConstraintValidatorContext context) {
        Optional<WriteCustomFieldDTO> customFieldFound = Optional.empty();
        var filedToCheck =  customFields.stream()
                .filter(customField -> customField.getName().compareToIgnoreCase(customFieldName)==0)
                .findFirst();
        // check if field has been set
        if (filedToCheck.isPresent() && writeCustomFieldList != null) {
            customFieldFound = writeCustomFieldList.stream()
                    .filter(customField1 -> customField1.id().compareTo(filedToCheck.get().getId()) == 0)
                    .findFirst();
           if(customFieldFound.isEmpty()) {
               context.buildConstraintViolationWithTemplate("The custom field '%s' is required".formatted(customFieldName))
                       .addPropertyNode(customFieldName)
                       .addConstraintViolation();
           }
        }
        return customFieldFound;
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param fieldValue  the value of the field to check
     * @param fieldName the name of the field to check
     */
    protected boolean checkStringField(String fieldValue, String fieldName, ConstraintValidatorContext context) {
        boolean isValid = true;
        if (fieldValue == null || fieldValue.isEmpty()) {
            isValid = false;
            context.buildConstraintViolationWithTemplate("The field '%s' is required".formatted(fieldName))
                    .addPropertyNode(fieldName)
                    .addConstraintViolation();
        }
        return isValid;
    }
}
