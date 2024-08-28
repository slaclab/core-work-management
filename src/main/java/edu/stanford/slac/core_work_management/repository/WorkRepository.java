package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkflowStateDTO;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkTypeStatusStatistics;
import edu.stanford.slac.core_work_management.model.workflow.WorkflowState;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Work objects
 */
@JaversSpringDataAuditable
public interface WorkRepository extends MongoRepository<Work, String>, WorkRepositoryCustom {
    @Aggregation(pipeline = {
            "{ '$group': { '_id': { 'workTypeId': '$workTypeId', 'status': '$currentStatus.status' }, 'count': { '$sum': 1 } } }",
            "{ '$group': { '_id':'$_id.workTypeId',  'status': { $addToSet: {status: '$_id.status', count: '$count'} } } }",
            "{ '$addFields': { 'workTypeId': '$_id' } }",
            "{ '$project': { '_id': 0, 'workTypeId': 1, 'status': 1 } }",
            "{ '$sort': { 'workTypeId': 1 } }"
    })
    List<WorkTypeStatusStatistics> getWorkStatusStatistics();

    @Aggregation(pipeline = {
            "{ $match: { domainId:?0, workTypeId: { $exists: true }, 'currentStatus.status': { $exists: true } } }",
            "{ '$group': { '_id': { 'workTypeId': '$workTypeId', 'status': '$currentStatus.status' }, 'count': { '$sum': 1 } } }",
            "{ '$group': { '_id':'$_id.workTypeId',  'status': { $addToSet: {status: '$_id.status', count: '$count'} } } }",
            "{ '$addFields': { 'workTypeId': '$_id' } }",
            "{ '$project': { '_id': 0, 'workTypeId': 1, 'status': 1 } }",
            "{ '$sort': { 'workTypeId': 1 } }"
    })
    List<WorkTypeStatusStatistics> getWorkStatisticsByDomainId(String domainId);

    /**
     * Find a work by domain id and id.
     *
     * @param domainId the domain id
     * @param id       the id
     * @return the work
     */
    Optional<Work> findByDomainIdAndId(String domainId, String id);

    /**
     * The Statistic for the work status count.
     *
     * @param workTypeId the work type id
     * @param status     the status
     * @return the count of work
     */
    long countByWorkTypeIdAndCurrentStatus_StatusIs(String workTypeId, WorkflowState status);

    /**
     * The Statistic for the work status count.
     *
     * @param domainId   the domain id
     * @param workTypeId the work type id
     * @param status     the status
     * @return the count of work
     */
    long countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(String domainId, String workTypeId, WorkflowState status);

    /**
     * Check for the work existence.
     *
     * @param domainId the domain id
     * @param id       the id
     * @return true if the work exists
     */
    boolean existsByDomainIdAndId(String domainId, String id);
}
