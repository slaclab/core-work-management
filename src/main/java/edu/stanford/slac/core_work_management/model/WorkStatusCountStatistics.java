package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
/**
 * Define the statistics of the work status count
 * correlate the number of work that are in a specific status
 */
public class WorkStatusCountStatistics {
    private WorkStatus status;
    private Integer count;
}
