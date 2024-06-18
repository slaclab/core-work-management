package edu.stanford.slac.core_work_management.model;

import lombok.*;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BucketSlotActivity {
    @Id
    private String id;
    private String activityId;
    private String bucketSlotId;
    @Builder.Default
    private BucketSlotActivityStatus status = BucketSlotActivityStatus.PENDING;
    @CreatedDate
    LocalDateTime createdDate;
    @CreatedBy
    String createdBy;
    @LastModifiedDate
    LocalDateTime lastModifiedDate;
    @LastModifiedBy
    String lastModifiedBy;
    @Version
    Long version;
}
