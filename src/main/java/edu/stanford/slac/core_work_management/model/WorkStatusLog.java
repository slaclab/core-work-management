package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkStatusLog {
    private WorkStatus status;
    @LastModifiedDate
    private LocalDateTime changed_on;
    @LastModifiedBy
    private String changed_by;
}
