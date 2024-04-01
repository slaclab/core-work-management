package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.WorkType;

import java.util.Optional;

public interface WorkTypeRepositoryCustom {
    /**
     * Ensure that the work type is valid
     *
     * @param activityType the work type
     * @return the work type
     */
    String ensureWorkType(WorkType activityType);

    Optional<WATypeCustomField> findCustomFieldById(String workTypeId, String customFieldId);
}
