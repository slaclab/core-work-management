package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Work;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for Work objects
 */
public interface WorkRepository extends MongoRepository<Work, String>,WorkRepositoryCustom {
}
