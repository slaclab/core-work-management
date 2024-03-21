package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.ActivityTypeCustomField;

import java.util.Optional;

public interface ActivityTypeRepositoryCustom {
    String ensureActivityType(ActivityType activityType);

    Optional<ActivityTypeCustomField> findCustomFiledById(String activityTypeId, String customFieldId);
}
