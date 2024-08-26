package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Work;

import java.util.Set;

/**
 * Base class for all workflows
 */
public abstract class BaseWorkflow {
    /**
     * Update the workflow  with the model with all the activities
     * @param work the work to update
     */
    abstract public void updateWithModel(Work work);
}
