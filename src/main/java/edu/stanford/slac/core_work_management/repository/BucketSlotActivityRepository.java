package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.BucketSlotActivity;
import edu.stanford.slac.core_work_management.model.BucketSlotActivityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BucketSlotActivityRepository extends MongoRepository<BucketSlotActivity, String> {
    boolean existsByBucketSlotIdAndActivityId(String bucketSlotId, String activityId);
    boolean existsByActivityIdAndStatusIn(String activityId, List<BucketSlotActivityStatus> status);
}
