package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityTypeRepository extends MongoRepository<ActivityType, String>, ActivityTypeRepositoryCustom {
    List<ActivityType> findAllByWorkTypeIdIs(String workTypeId);
}
