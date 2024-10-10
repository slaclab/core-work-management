package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a BucketSlotWorkType entity in the system.
 * The BucketSlotWorkType class is used to store information about a specific bucket slot work type.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class BucketSlotWorkType {
    /**
     * The id of the bucket slot work type
     */
    private String domainId;
    /**
     * The id of the bucket slot work type
     */
    private String workTypeId;
}
