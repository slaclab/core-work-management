package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityStatus;
import edu.stanford.slac.core_work_management.model.Work;

import java.util.Set;

/**
 * Base class for all workflows
 */
public abstract class BaseWorkflow {
    /**
     * Update the workflow  with the model with all the activities
     * @param work the work to update
     * @param activities the activity related to the work
     */
    abstract public void updateWithModel(Work work, Set<Activity> activities);


    /**
     * Helper method to check if all activities are completed or dropped
     * @param activities the set of activities
     * @return true if all activities are completed or dropped
     */
    protected boolean areAllActivitiesCompleteOrDrop(Set<ActivityStatus> activities) {
        return activities.stream().allMatch(activity -> activity == ActivityStatus.Completed || activity == ActivityStatus.Drop);
    }

    /**
     * Helper method to check if any activity is in a specific status
     * @param activities the set of activities
     * @param activityStatus the status to check
     * @return true if any activity is in the status
     */
    protected boolean isAnyActivityInStatus(Set<ActivityStatus> activities, ActivityStatus activityStatus) {
        return activities.stream().anyMatch(activity -> activity == activityStatus);
    }
}
