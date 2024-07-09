package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatus;
import edu.stanford.slac.core_work_management.model.WorkTypeStatusStatistics;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository for Work objects
 */
@JaversSpringDataAuditable
public interface WorkRepository extends MongoRepository<Work, String>,WorkRepositoryCustom {
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

    long countByWorkTypeIdAndCurrentStatus_StatusIs(String workTypeId, WorkStatus status);
    long countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(String domainId, String workTypeId, WorkStatus status);
}
