package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityStatusLog {
    private ActivityStatus status;
    @CreatedDate
    private LocalDateTime changed_on;
    @CreatedBy
    private String changed_by;
}
