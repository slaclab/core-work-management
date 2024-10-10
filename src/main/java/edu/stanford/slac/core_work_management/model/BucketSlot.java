package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.model.value.LOVField;
import lombok.*;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

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
     * The list of domain that can use this bucket slot
     */
    private Set<String> domainIds;

    /**
     * The description of the bucket slot
     */
    private String description;
    /**
     * The id of the lov value used to define the bucket status
     */
    @LOVField(fieldReference = "type", isMandatory = true)
    private String type;
    /**
     * The id of the lov value used to define the bucket status
     */
    @LOVField(fieldReference = "status", isMandatory = true)
    private String status;
    /**
     * The start date and time of the bucket slot
     */
    private LocalDateTime from;
    /**
     * The end date and time of the bucket slot
     */
    private LocalDateTime to;
    /**
     * The id of the work type admitted to the bucket slot
     */
    private Set<BucketSlotWorkType> admittedWorkTypeIds;
    /**
     * The created date of the bucket slot
     */
    @CreatedDate
    private LocalDateTime createdDate;
    /**
     * The created by of the bucket slot
     */
    @CreatedBy
    private String createdBy;
    /**
     * The last modified date of the bucket slot
     */
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    /**
     * The last modified by of the bucket slot
     */
    @LastModifiedBy
    private String lastModifiedBy;
    /**
     * The version of the bucket slot
     */
    @Version
    private Long version;
    /**
     * The processing id of the bucket slot
     */
    private String processingId;
    /**
     * The processing time of the bucket slot
     */
    private LocalDateTime processingTimestamp;
    /**
     * Sign that the bucket slot has been managed for the started
     * event, typically workflow are updated for each work associated to it
     */
    @Builder.Default
    private Boolean startEventManaged = false;
    /**
     * Sign that the bucket slot has been managed for the stop
     * event, typically workflow are updated for each work associated to it
     */
    @Builder.Default
    private Boolean stopEventManaged = false;
}
