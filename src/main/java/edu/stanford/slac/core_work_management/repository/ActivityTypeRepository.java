package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityTypeRepository extends MongoRepository<ActivityType, String>, ActivityTypeRepositoryCustom {
    /**
     * Find all the activity types by domain and work id
     *
     * @param domainId the domain id
     * @param workId   the work id
     * @return the list of activity types
     */
    List<ActivityType> findAllByDomainIdAndWorkTypeId(String domainId, String workId);

    /**
     * Find the activity type by domain, work and id
     *
     * @param domainId   the domain id
     * @param workTypeId the work id
     * @param id         the id
     * @return the activity type
     */
    Optional<ActivityType> findByDomainIdIsAndWorkTypeIdIsAndIdIs(String domainId, String workTypeId, String id);

}
