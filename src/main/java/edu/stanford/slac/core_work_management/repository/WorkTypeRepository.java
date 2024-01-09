package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.WorkType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkTypeRepository extends MongoRepository<WorkType, String>, WorkTypeRepositoryCustom {
}
