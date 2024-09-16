package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Define a shop group that is composed by multiple user id
 * Is a shop group is used to define which users should be
 * notified for execute the work/activity
 */
@Data
@Builder(toBuilder = true)
public class EmbeddableShopGroup {
    String id;
    /**
     * The domain id where the shop group belong to
     */
    String domainId;
    /**
     * The name of the shop group
     */
    String name;
    /**
     * The user ids that are part of the shop group
     */
    Set<ShopGroupUser> users;
}
