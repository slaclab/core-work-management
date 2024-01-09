package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

/**
 * Represents an activity associated with a work. This class encapsulates all the
 * details of an activity
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Activity {
    // This map defines the valid transitions for each state
    static ActivityStatusStateMachine activityStatusStateMachine = new ActivityStatusStateMachine();
    /**
     * The unique identifier for the activity.
     * This field is annotated with @Id to indicate that it's the primary key.
     */
    @Id
    private String id;

    /**
     * The identifier of the related work.
     * This field links the activity to its corresponding work.
     */
    private String workId;

    /**
     * The title of the activity.
     * This field stores the title or name of the activity.
     */
    private String name;

    /**
     * The detailed description of the activity.
     * This field provides a comprehensive description of what the activity entails.
     */
    private String description;

    /**
     * The type of the activity.
     * This field categorizes the activity into a specific type, defined by the ActivityType enum.
     */
    private ActivityType type;

    /**
     * Is the actual status of the activity.
     */
    @Builder.Default
    private ActivityStatusLog currentStatus = ActivityStatusLog.builder().status(ActivityStatus.New).build();

    /**
     * Is the full activity status history
     */
    private List<ActivityStatusLog> statusHistory;

    /**
     * The date and time when the activity was created.
     * This field is automatically populated with the creation date and time, using @CreatedDate annotation.
     */
    @CreatedDate
    private LocalDateTime createdDate;

    /**
     * The identifier of the user who created the activity.
     * This field stores the ID of the user who initially created the activity, using @CreatedBy annotation.
     */
    @CreatedBy
    private String createdBy;

    /**
     * The date and time when the activity was last modified.
     * This field is automatically updated with the date and time of the last modification, using @LastModifiedDate annotation.
     */
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    /**
     * The identifier of the user who last modified the activity.
     * This field stores the ID of the user who made the last modification to the activity, using @LastModifiedBy annotation.
     */
    @LastModifiedBy
    private String lastModifiedBy;

    /**
     * The version of the activity entity.
     * This field is used for optimistic locking and is automatically managed by the database, using @Version annotation.
     */
    @Version
    private Long version;

    /**
     * The list of the identifiers of the users who are assigned to the activity.
     * This field stores the IDs of the users who are assigned to the activity.
     *
     * @param newStatus the new status to transition to
     */
    public void setStatus(ActivityStatus newStatus) {
        assertion(
                () -> activityStatusStateMachine.transitionTo(currentStatus.getStatus(), newStatus),
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage(String.format("Invalid status transition from %s to %s", currentStatus.getStatus(), newStatus))
                        .errorDomain("Activity")
                        .build()

        );
        activityStatusStateMachine.transitionTo(currentStatus.getStatus(), newStatus);
        currentStatus = ActivityStatusLog.builder()
                .status(newStatus)
                .build();
    }

}
