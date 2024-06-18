package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.model.value.LOVField;
import lombok.*;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BucketSlot {
    /**
     * The id of the bucket slot
     */
    @Id
    private String id;
    /**
     * The description of the bucket slot
     */
    private String description;
    /**
     * The id of the lov value used to define the bucket type
     */
    @LOVField(fieldReference = "bucketType", isMandatory = true)
    /**
     * The id of the lov value used to define the bucket status
     */
    private String bucketType;
    /**
     * The id of the lov value used to define the bucket status
     */
    @LOVField(fieldReference = "bucketStatus", isMandatory = true)
    private String bucketStatus;
    /**
     * The start date and time of the bucket slot
     */
    private LocalDateTime from;
    /**
     * The end date and time of the bucket slot
     */
    private LocalDateTime to;
    /**
     * The created date of the bucket slot
     */
    @CreatedDate
    LocalDateTime createdDate;
    /**
     * The created by of the bucket slot
     */
    @CreatedBy
    String createdBy;
    /**
     * The last modified date of the bucket slot
     */
    @LastModifiedDate
    LocalDateTime lastModifiedDate;
    /**
     * The last modified by of the bucket slot
     */
    @LastModifiedBy
    String lastModifiedBy;
    /**
     * The version of the bucket slot
     */
    @Version
    Long version;
}
