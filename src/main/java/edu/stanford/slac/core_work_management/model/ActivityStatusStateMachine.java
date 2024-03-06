package edu.stanford.slac.core_work_management.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Activity Status state machine
 * This class is used to validate the state transitions of an Activity
 */
public class ActivityStatusStateMachine {
    // This map defines the valid transitions for each state
    static public Map<ActivityStatus, Set<ActivityStatus>> validTransitions = Map.of(
            ActivityStatus.New, Set.of(ActivityStatus.Completed, ActivityStatus.Approved, ActivityStatus.Drop, ActivityStatus.Roll),
            ActivityStatus.Approved, Set.of(ActivityStatus.Completed, ActivityStatus.Drop, ActivityStatus.Roll),
            ActivityStatus.Roll, Set.of(ActivityStatus.Drop, ActivityStatus.Completed),
            ActivityStatus.Completed, Set.of(),
            ActivityStatus.Drop, Set.of()
    );


    /**
     * Check if the transition is valid
     * @param currentState the current state
     * @param newState the new state
     * @return true if the transition is valid, false otherwise
     */
    synchronized public boolean isValidTransition(ActivityStatus currentState, ActivityStatus newState) {
        return validTransitions.getOrDefault(currentState, Collections.emptySet()).contains(newState);
    }

    /**
     * Get the list of available states from the current state
     *
     * @param currentState the current state
     * @return the list of available states
     */
    synchronized public Set<ActivityStatus> getAvailableState(ActivityStatus currentState) {
        return validTransitions.getOrDefault(currentState, Collections.emptySet());
    }
}
