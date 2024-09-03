package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
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
                // Rule: workStatus = "Approved"
                WorkflowState.Submitted, Set.of(WorkflowState.Approved),
                // Rule: becomes active when startDate starts
                WorkflowState.Approved, Set.of(WorkflowState.InProgress),
                // Rule: becomes active when endDate reaches or user tag completed the work
                WorkflowState.InProgress, Set.of(WorkflowState.WorkComplete),
                // user/admin automatically close the work
                WorkflowState.WorkComplete, Set.of(WorkflowState.ReviewToClose),
                // Rule: the area manager close the work
                WorkflowState.ReviewToClose, Set.of(WorkflowState.Closed)
        );
    }
    @Override
    public void update(Work work, UpdateWorkflowState updateWorkflowState) {

    }

    @Override
    public void canUpdate(String identityId, Work work) {
    }

    @Override
    public boolean canCreateChild(Work work) {
        return false;
    }

    @Override
    public boolean isCompleted(Work work) {
        return false;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
