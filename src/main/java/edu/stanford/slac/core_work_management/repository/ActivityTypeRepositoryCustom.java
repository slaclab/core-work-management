package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;

public interface ActivityTypeRepositoryCustom {
    public String ensureActivityType(ActivityType activityType);
}
