package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Domain;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DomainRepository  extends MongoRepository<Domain, String> {
}
