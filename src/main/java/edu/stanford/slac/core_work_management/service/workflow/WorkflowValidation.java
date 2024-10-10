package edu.stanford.slac.core_work_management.service.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowValidation<T>{
    private T value;
    private BaseWorkflow workflow;
}
