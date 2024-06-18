package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.BucketSlot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BucketRepository extends MongoRepository<BucketSlot, String>, BucketRepositoryCustom {

}
