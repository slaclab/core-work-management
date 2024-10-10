package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.WorkType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkTypeRepository extends MongoRepository<WorkType, String>, WorkTypeRepositoryCustom {
    /**
     * Find all the work types by domain id.
     *
     * @param domainId the domain id
     * @return the list of work types
     */
    List<WorkType> findAllByDomainId(String domainId);

    /**
     * Find a work type by domain id and id.
     *
     * @param domainId the domain id
     * @param id the id
     * @return the work type
     */
    Optional<WorkType> findByDomainIdAndId(String domainId, String id);

    /**
     * Check if a work type exists by domain id and id.
     *
     * @param domainId the domain id
     * @param id the id
     * @return true if the work type exists, false otherwise
     */
    boolean existsByDomainIdAndId(String domainId, String id);
}
