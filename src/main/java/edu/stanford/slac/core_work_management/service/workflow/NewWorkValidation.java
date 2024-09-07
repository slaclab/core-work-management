package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.model.WorkType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewWorkValidation {
    private NewWorkDTO newWorkDTO;
    private WorkType workType;
}
