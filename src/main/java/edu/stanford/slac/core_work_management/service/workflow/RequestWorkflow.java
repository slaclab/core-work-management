package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;


@Workflow(
        name = "Request",
        description = "The workflow for a request"
)
@Component("RequestWorkflow")
public class RequestWorkflow extends BaseWorkflow {
    public RequestWorkflow() {
        validTransitions = Map.of(
                // Rule: Work gets put into a bucket
                WorkflowState.Created, Set.of(WorkflowState.PendingApproval),

                // Rule: Area manager approves, if a safety form = true, then it needs to be attached
                WorkflowState.PendingApproval, Set.of(WorkflowState.ReadyForWork),

                // Rule: Start date has begun
                WorkflowState.ReadyForWork, Set.of(WorkflowState.InProgress),

                // Rule: Work is marked as complete by user, admin gets notified
                WorkflowState.InProgress, Set.of(WorkflowState.WorkComplete),

                // Rule: Admin manually closes
                WorkflowState.WorkComplete, Set.of(WorkflowState.Closed)
        );
    }
}
