package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * The workflow logic for a record type of work
 */
@Workflow(
        name = "Record",
        description = "The workflow logic for a record type of work"
)
@Component("RecordWorkflow")
public class RecordWorkflow extends BaseWorkflow {

    // This map defines the valid transitions for each state
    public RecordWorkflow() {
        validTransitions = Map.of(
                WorkflowState.Created, Set.of(WorkflowState.Closed),
                WorkflowState.Closed, Set.of()
        );
    }
}
