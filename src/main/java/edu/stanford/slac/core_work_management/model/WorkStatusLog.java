package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.service.workflow.WorkflowState;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;


/**
 * Define the work status log
 * correlate the status of the work and the user that changed the status
 */
@Data
@Builder
public class WorkStatusLog {
    private WorkflowState status;
    @LastModifiedDate
    private LocalDateTime changed_on;
    @LastModifiedBy
    private String changed_by;
}
