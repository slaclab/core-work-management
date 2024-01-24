package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Set;

/**
 * Define a shop group that is composed by multiple user id
 * Is a shop group is used to define which users should be
 * notified for execute the work/activity
 */
@Data
@Builder
public class ShopGroup {
    @Id
    String id;
    String name;
    String description;
    Set<String> userEmails;
}
