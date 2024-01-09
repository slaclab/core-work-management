package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

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
     * The name of the work.
     * This field stores the name or title of the work.
     */
    private String name;

    /**
     * The description of the work.
     * This field provides detailed information about the work.
     */
    private String description;

    /**
     * The identifier of the location associated with the work.
     * This field links the work to a specific location, identified by its ID.
     */
    private String locationId;

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

