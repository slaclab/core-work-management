package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class LogEntry {
    String id;
    String title;
    String text;
    String relationId;
    List<String> attachmentIds;
    LocalDateTime eventAt;
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
