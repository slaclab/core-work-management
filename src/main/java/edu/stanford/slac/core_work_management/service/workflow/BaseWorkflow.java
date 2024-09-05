package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatusLog;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     * @param work the work to check
     */
    abstract public void isValid(Work work);

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
        if(newState == null) {
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
                ()-> validTransitions != null && validTransitions.get(work.getCurrentStatus().getStatus()).contains(Objects.requireNonNullElse(newState.getNewState(), WorkflowState.None))
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
}
