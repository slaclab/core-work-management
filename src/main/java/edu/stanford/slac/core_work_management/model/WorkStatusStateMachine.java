package edu.stanford.slac.core_work_management.model;

import java.util.Set;

public class WorkStatusStateMachine {

    // Method to handle state transitions based on the list of activities
    public WorkStatus getNewStatus(WorkStatus currentStatus, Set<ActivityStatus> activities) {
        WorkStatus status = currentStatus;
        switch (currentStatus) {
            case WorkStatus.New:
                if (activities.stream().anyMatch(activity -> activity == ActivityStatus.New)) {
                    status = WorkStatus.InProgress;
                }
                break;
            case WorkStatus.InProgress:
                if (areAllActivitiesComplete(activities)) {
                    status = WorkStatus.Review;
                } else if (isAnyActivityRolled(activities)) {
                    status = WorkStatus.ScheduledJob;
                }
                break;
            // Other cases...
        }

        return status;
    }

    // Helper method to check if all activities are complete
    private boolean areAllActivitiesComplete(Set<ActivityStatus> activities) {
        return activities.stream().allMatch(activity -> activity == ActivityStatus.Completed);
    }

    // Helper method to check if any activity is rolled
    private boolean isAnyActivityRolled(Set<ActivityStatus> activities) {
        return activities.stream().anyMatch(activity -> activity == ActivityStatus.InProgress);
    }
}
