package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.EventTrigger;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface EventTriggerRepository   extends MongoRepository<EventTrigger, String>, EventTriggerRepositoryCustom {
}
