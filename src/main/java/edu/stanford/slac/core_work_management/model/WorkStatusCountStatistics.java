package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WorkStatusCountStatistics {
    private WorkStatus status;
    private Integer count;
}
