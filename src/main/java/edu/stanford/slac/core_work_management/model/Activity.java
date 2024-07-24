package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.model.value.LOVField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * The unique identifier for the domain.
     * This field is used to identify the domain to which the work belongs.
     */
    private String domainId;
    /**
     * The unique identifier for the activity.
     * This field is autogenerated on the sum of activity present in the work
     */
    private Long activityNumber;

    /**
     * The identifier of the related work.
     * This field links the activity to its corresponding work.
     */
    private String workId;

    /**
     * The unique identifier for the work number.
     * This field is autogenerated on the sum of activity present in the work
     */
    private Long workNumber;

    /**
     * The title of the activity.
     * This field stores the title or name of the activity.
     */
    private String title;

    /**
     * The detailed description of the activity.
     * This field provides a comprehensive description of what the activity entails.
     */
    private String description;
    /**
     * The type of the activity.
     * This field categorizes the activity into a specific type, defined by the ActivityType enum.
     */
    private String activityTypeId;
    /**
     * The subtype of the activity.
     * This field further categorizes the activity into a specific subtype, defined by the ActivityTypeSubtype enum.
     */
    private ActivityTypeSubtype activityTypeSubtype;
    /**
     * The list of the identifiers of the users who are assigned to the activity.
     * This field stores the IDs of the users who are assigned to the activity.
     */
    private List<String> assignedTo;
    /**
     * The location of the activity.
     * This field stores the location of the activity.
     */
    private String locationId;
    /**
     * The shop group that performs the work in the location.
     * This field stores the shop group that performs the work in the location.
     */
    private String shopGroupId;
    /**
     * The alternative shop group that performs the work in the location.
     * This field stores the alternative shop group that performs the work in the location.
     */
    private String alternateShopGroupId;
    /**
     * The planned start date of the activity.
     * This field stores the planned start date and time of the activity.
     */
    private LocalDateTime plannedStartDate;
    /**
     * The planned end date of the activity.
     * This field stores the planned end date and time of the activity.
     */
    private LocalDateTime plannedEndDate;
    /**
     * The id of the lov value used to define the project
     */
    @LOVField(fieldReference = "project", isMandatory = true)
    private String project;
    /**
     * The list of the custom fields associated with the activity.
     * The custom fields are used to store additional information about the activity.
     */
    private List<CustomField> customFields;

    /**
     * Is the actual status of the activity.
     */
    @Builder.Default
    private ActivityStatusLog currentStatus = ActivityStatusLog.builder().status(ActivityStatus.New).build();

    /**
     * Is the full activity status history
     */
    @Builder.Default
    private List<ActivityStatusLog> statusHistory = new ArrayList<>();

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
     * @param newStatus           the new status to transition to
     * @param followupDescription the followup description
     */
    public void setStatus(ActivityStatus newStatus, String followupDescription) {
        ActivityStatusLog currentStatus = getCurrentStatus();
        assertion(
                () -> activityStatusStateMachine.isValidTransition(currentStatus.getStatus(), newStatus),
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage(String.format("Invalid status transition from %s to %s", currentStatus.getStatus(), newStatus))
                        .errorDomain("Activity::setStatus")
                        .build()

        );
        setCurrentStatus(
                ActivityStatusLog.builder()
                        .status(newStatus)
                        .followUpDescription(followupDescription)
                        .build()
        );
        getStatusHistory().addFirst(currentStatus);
    }

    /**
     * Get the list of available states from the current state
     *
     * @return the list of available states
     */
    public List<ActivityStatus> getAvailableStatus() {
        return activityStatusStateMachine.getAvailableState(currentStatus.getStatus()).stream().toList();
    }

}
