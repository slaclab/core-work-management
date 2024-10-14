package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewWorkValidation {
    private Work work;
}
