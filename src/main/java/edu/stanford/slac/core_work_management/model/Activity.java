package edu.stanford.slac.core_work_management.model;

import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

public class Activity {
    @Id
    private String id;
    /**
     * Is the id of the related work
     */
    private String workId;
    /**
     * Is the title of the activity
     */
    private String title;
    /**
     *  Is the activity description
     */
    private String description;

    /**
     * Is the activity type
     */
    private ActivityType type;
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
