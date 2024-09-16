package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.service.workflow.WorkflowState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Work entity in the system.
 * The Work class is used to store information about a specific work. A work can be related to a specific location
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Work {
    /**
     * The unique identifier for the work.
     * This field is annotated with @Id to signify its use as a primary key in MongoDB.
     */
    @Id
    private String id;
    /**
     * The parent work id if the work is a sub work.
     * This field is used to identify the parent work of the current work.
     */
    private String parentWorkId;
    /**
     * The unique identifier for the domain.
     * This field is used to identify the domain to which the work belongs.
     */
    private String domainId;
    /**
     * The unique identifier for the work.
     * This field is autogenerated on the sum of activity present in the work
     */
    private Long workNumber;
    /**
     * The type of the work.
     * This field identify the work which this is related to. This creates dependency between works
     */
    private List<String> relatedToWorkIds;
    /**
     * The type of the work.
     * This field categorizes the work into a specific type
     */
    private EmbeddableWorkType workType;
    /**
     * The name of the work.
     * This field stores the name or title of the work.
     */
    private String title;
    /**
     * The description of the work.
     * This field provides detailed information about the work.
     */
    private String description;
    /**
     * The location where the work is performed.
     */
    private EmbeddableLocation location;
    /**
     * The shop group that perform the work in the location
     */
    private EmbeddableShopGroup shopGroup;
    /**
     * The identifier of the user assigned to the work.
     * This field links the work to a specific user, identified by its ID.
     */
    private List<String> assignedTo;

    /**
     * The list of the custom fields associated with the activity.
     * The custom fields are used to store additional information about the activity.
     */
    private List<CustomField> customFields;

    /**
     * The list of the attachments associated with the work.
     */
    private List<String> attachments;

    /**
     * Is the actual status of the work.
     */
    @Builder.Default
    private WorkStatusLog currentStatus = WorkStatusLog.builder().status(WorkflowState.Created).build();

    /**
     * The list of the notifications associated with the work.
     */
    @Builder.Default
    private List<Notification> notificationsHistory = new ArrayList<>();

    /**
     * Is the full work status history
     */
    @Builder.Default
    private List<WorkStatusLog> statusHistory = new ArrayList<>();

    /**
     * The date and time when the work was created.
     * This field is automatically populated with the date and time of creation, using @CreatedDate annotation.
     */
    @CreatedDate
    private LocalDateTime createdDate;

    /**
     * The identifier of the user who created the work.
     * This field stores the ID of the user who initially created the work, using @CreatedBy annotation.
     */
    @CreatedBy
    private String createdBy;

    /**
     * The date and time when the work was last modified.
     * This field is automatically updated with the date and time of the last modification, using @LastModifiedDate annotation.
     */
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    /**
     * The identifier of the user who last modified the work.
     * This field stores the ID of the user who made the last modification to the work, using @LastModifiedBy annotation.
     */
    @LastModifiedBy
    private String lastModifiedBy;

    /**
     * The version of the work entity.
     * This field is used for optimistic locking and is automatically managed by MongoDB, using @Version annotation.
     */
    @Version
    private Long version;
}

