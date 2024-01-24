package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ShopGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShopGroupRepository extends MongoRepository<ShopGroup, String>{
}
