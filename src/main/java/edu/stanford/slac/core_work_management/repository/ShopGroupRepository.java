package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ShopGroup;
import jakarta.validation.constraints.NotNull;
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
     * @param userEmail the email of the user
     * @return true if the shop group exists
     */
    public boolean existsByIdAndUserEmailsContainingIgnoreCase(@NonNull String shopGroupId, @NonNull String userEmail);
}
