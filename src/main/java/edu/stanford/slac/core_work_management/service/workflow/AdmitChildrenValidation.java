package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdmitChildrenValidation {
    private Work work;
    private WorkType workType;
    private BaseWorkflow workflow;
}
