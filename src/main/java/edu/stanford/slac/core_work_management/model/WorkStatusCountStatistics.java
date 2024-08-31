package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.service.workflow.WorkflowState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


/**
 * Define the statistics of the work status count
 * correlate the number of work that are in a specific status
 */
@Data
@Builder
@AllArgsConstructor
public class WorkStatusCountStatistics {
    /**
     * The status of the work for the statistic
     */
    private WorkflowState status;
    /**
     * The count of the work that are in the status
     */
    private Integer count;
}
