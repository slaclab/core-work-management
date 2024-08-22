package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ActivityType model
 *
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ActivityType {
    @Id
    private String id;
    /**
     * Refer to which domain this activity type belongs to.
     */
    private String domainId;
    /**
     * Refer to which work type this activity type belongs to.
     */
    private String workTypeId;
    /**
     * The title of the activity type.
     * This field stores the title or name of the activity type.
     */
    private String title;
    /**
     * The detailed description of the activity type.
     * This field provides a comprehensive description of what the activity type entails.
     */
    private String description;

    /**
     * The list of activity types in the activity type.
     */
    private Set<ActivityTypeSubtype> activityTypeSubtypes;

    /**
     * The list of the custom fields associated with the activity type.
     * The custom fields are used to store additional information about the activity.
     */
    @Builder.Default
    private List<WATypeCustomField> customFields = new ArrayList<>();
    @CreatedDate
    private LocalDateTime createdDate;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    @LastModifiedBy
    private String lastModifiedBy;
    @Version
    private Long version;
}
