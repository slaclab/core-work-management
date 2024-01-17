package edu.stanford.slac.core_work_management.model;

import java.time.LocalDateTime;
import java.util.Set;

public class WorkStatusStateMachine {

    // Method to handle state transitions based on the list of activities
    public void updateModel(Work work, Set<ActivityStatus> activities) {
        WorkStatusLog currentStatus = work.getCurrentStatus();
        WorkStatus nextStatus = null;
        if (areAllActivitiesCompleteOrDrop(activities)) {
            nextStatus = WorkStatus.Review;
        } else if (isAnyActivityInStatus(activities, ActivityStatus.Roll)) {
            nextStatus = WorkStatus.ScheduledJob;
        } else if (isAnyActivityInStatus(activities, ActivityStatus.New)) {
            nextStatus = WorkStatus.ScheduledJob;
        } else if(work.getAssignedTo()!=null) {
            nextStatus = WorkStatus.InProgress;
        }
        if(nextStatus!=null) {
            // update current status and log the old one
            work.getStatusHistory().addFirst(currentStatus);
            work.setCurrentStatus(
                    WorkStatusLog.builder()
                    .status(nextStatus)
                    .changed_by(currentStatus.getChanged_by())
                    .changed_on(LocalDateTime.now())
                    .build()
            );
        }
    }

    // Helper method to check if all activities are complete
    private boolean areAllActivitiesCompleteOrDrop(Set<ActivityStatus> activities) {
        return activities.stream().allMatch(activity -> activity == ActivityStatus.Completed || activity == ActivityStatus.Drop);
    }

    // Helper method to check if any activity is rolled


    private boolean isAnyActivityInStatus(Set<ActivityStatus> activities, ActivityStatus activityStatus) {
        return activities.stream().anyMatch(activity -> activity == activityStatus);
    }
}
