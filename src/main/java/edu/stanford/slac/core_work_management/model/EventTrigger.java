package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class EventTrigger {
    private String id;
    // the name of the trigger
    private String typeName;
    private String referenceId;
    private Object payload;
    private String processingId;
    private String processingTimeStamp;
    private LocalDateTime eventFireTimestamp;
    @Builder.Default
    private Boolean fired = false;
    @CreatedDate
    private LocalDateTime createdDate;
    @CreatedBy
    private String createdBy;
}
