package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityStatus;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityRepository extends MongoRepository<Activity, String>, ActivityRepositoryCustom {
    @Aggregation(pipeline = {
            "{ $match: { 'workId': ?0 } }",
            "{ $project: { 'currentStatus': 1, '_id': 0 } }",
            "{ $replaceRoot: { newRoot: '$currentStatus' } }",
    })
    List<ActivityStatusLog> findAllActivityStatusByWorkId(String workId);

    List<Activity> findAllByWorkId(String workId);
}
