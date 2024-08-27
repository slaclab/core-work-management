package edu.stanford.slac.core_work_management.model.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.util.Map;
import java.util.Set;

/**
 * Base class for all workflows
 */
@Data
public abstract class BaseWorkflow {
    protected Map<WorkflowState, Set<WorkflowState>> validTransitions;
    /**
     * Update the workflow  with the model with all the activities
     * @param work the work to update
     */
    abstract public void updateWithModel(Work work);
}
