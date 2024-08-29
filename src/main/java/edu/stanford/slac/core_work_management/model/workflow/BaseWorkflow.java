package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.model.Work;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     * @param work     the work to update
     * @param newState the state to move to, this is optional and can be null
     */
    abstract public void update(Work work, WorkflowState newState);

    /**
     * Check if the user can update the work
     *
     * @param authentication the user that is trying to update the work
     * @param work           the work to update
     * @param newState       the state to move to, this is optional and can be null
     */
    abstract public boolean canUpdate(Authentication authentication, Work work, WorkflowState newState);

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
    protected void canMoveToState(Work work, WorkflowState newState) {
        boolean canTransitioning = validTransitions != null && validTransitions.get(work.getCurrentStatus().getStatus()).contains(Objects.requireNonNullElse(newState, WorkflowState.None));
        if (canTransitioning) return;
        throw ControllerLogicException
                .builder()
                .errorCode(-1)
                .errorMessage("Cannot move to state %s from %s".formatted(newState.name(), work.getCurrentStatus().getStatus().name()))
                .errorDomain("DummyWorkflow::update")
                .build();
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
     * @param work the work to check
     * @param state the states to check
     * @return true if the status is equal to any of the provided states
     */
    protected boolean isStatusEqualTo(Work work, Set<WorkflowState> state) {
        if (work == null) return false;
        var currentStatus = work.getCurrentStatus().getStatus();
        return state.stream().anyMatch(s -> s == currentStatus);
    }
}
