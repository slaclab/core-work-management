package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.service.workflow.WorkflowState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Define the work status log
 * correlate the status of the work and the user that changed the status
 */
@Data
@Builder
@AllArgsConstructor
public class UpdateWorkflowState {
    /**
     * The status of the work
     */
    @NotNull
    WorkflowState newState;
    /**
     * The comment for the status change
     */
    String comment;
}
