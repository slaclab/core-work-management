package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * Represents an association of the work to a bucket.
 * The WorkBucketAssociation class is used to store information about a specific work bucket association.
 * a work can have many of these associations with rollUp flag set to true but only , last, set as false.
 * so in this way we can trace back the work to the bucket association and finally understand the bucket that
 * was used to execute the work
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
public class WorkBucketAssociation {
    /**
     * The unique identifier for the work bucket association.
     */
    private String bucketId;
    /**
     * The unique identifier for the work.
     */
    private Boolean rolled;
    @LastModifiedDate
    private LocalDateTime createdDate;
    @LastModifiedBy
    private String createdBy;
}
