package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.WorkType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkTypeRepository extends MongoRepository<WorkType, String>, WorkTypeRepositoryCustom {
    /**
     * Find all the work types by domain id.
     *
     * @param domainId the domain id
     * @return the list of work types
     */
    List<WorkType> findAllByDomainId(String domainId);
}
