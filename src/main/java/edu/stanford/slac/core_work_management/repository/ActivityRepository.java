package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityRepository extends MongoRepository<Activity, String> {
}
