package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.WorkType;

public interface WorkTypeRepositoryCustom {
    public String ensureWorkType(WorkType activityType);
}
