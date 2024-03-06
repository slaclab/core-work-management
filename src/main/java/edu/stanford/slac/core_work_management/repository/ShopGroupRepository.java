package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ShopGroup;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShopGroupRepository extends MongoRepository<ShopGroup, String>{
    /**
     * Check if a shop group exists
     *
     * @param shopGroupId the id of the shop group
     * @return true if the shop group exists
     */
    boolean existsById(@NonNull String shopGroupId);

    /**
     * Check if a specific shop group contains a user email
     *
     * @param shopGroupId the id of the shop group
     * @param userIds the email of the user
     * @return true if the shop group exists
     */
    boolean existsByIdAndUsers_User_uid_ContainingIgnoreCase(@NonNull String shopGroupId, @NonNull String userIds);
}
