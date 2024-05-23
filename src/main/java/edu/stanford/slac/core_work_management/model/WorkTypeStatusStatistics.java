package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class WorkTypeStatusStatistics {
    private String workTypeId;
    private List<WorkStatusCountStatistics> status;
}

