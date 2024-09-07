package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateWorkValidation {
    private UpdateWorkDTO updateWorkDTO;
    private Work existingWork;
    private WorkType workType;
}
