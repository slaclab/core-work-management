package edu.stanford.slac.core_work_management.service.validation;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.service.workflow.BaseWorkflow;

/**
 * The state of the validation
 */
public class ValidationData {
    Work work;
    WorkType workType;
    BaseWorkflow workflow;
}
