package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.BucketSlot;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BucketRepository extends MongoRepository<BucketSlot, String>, BucketRepositoryCustom {
}
