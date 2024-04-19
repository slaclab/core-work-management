package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.LogEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LogEntryRepository extends MongoRepository<LogEntry, String> {
}
