package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ShopGroup;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ShopGroupRepository extends MongoRepository<ShopGroup, String>{
    Optional<ShopGroup> findByDomainIdAndId(@NonNull String domainId, @NonNull String id);
    List<ShopGroup> findAllByDomainId(@NonNull String domainId);
    /**
     * Check if a shop group exists
     *
     * @param shopGroupId the id of the shop group
     * @return true if the shop group exists
     */
    boolean existsByDomainIdAndId(@NonNull String domainId, @NonNull String shopGroupId);

    /**
     * Check if a specific shop group contains a user email
     *
     * @param shopGroupId the id of the shop group
     * @param userIds the email of the user
     * @return true if the shop group exists
     */
    boolean existsByDomainIdAndIdAndUsers_User_mail_ContainingIgnoreCase(@NonNull String domainId, @NonNull String shopGroupId, @NonNull String userIds);
}
