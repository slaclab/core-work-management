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
    Map<ActivityStatus, Set<ActivityStatus>> validTransitions = Map.of(
            ActivityStatus.New, Set.of(ActivityStatus.Authorized, ActivityStatus.Rejected, ActivityStatus.Cancelled),
            ActivityStatus.Authorized, Set.of(ActivityStatus.InProgress, ActivityStatus.Cancelled),
            ActivityStatus.InProgress, Set.of(ActivityStatus.Completed, ActivityStatus.Cancelled),
            ActivityStatus.Completed, Set.of(),
            ActivityStatus.Rejected, Set.of(),
            ActivityStatus.Cancelled, Set.of()
    );

    synchronized public boolean transitionTo(ActivityStatus currentState, ActivityStatus newState) {
        if (isValidTransition(currentState, newState)) {
            return true;
        }
        return false;
    }

    synchronized private boolean isValidTransition(ActivityStatus currentState, ActivityStatus newState) {
        return validTransitions.getOrDefault(currentState, Collections.emptySet()).contains(newState);
    }
}
