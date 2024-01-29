package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ShopGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShopGroupRepository extends MongoRepository<ShopGroup, String>{
    /**
     * Check if a shop group exists
     *
     * @param shopGroupId the id of the shop group
     * @return true if the shop group exists
     */
    boolean existsById(String shopGroupId);
}
