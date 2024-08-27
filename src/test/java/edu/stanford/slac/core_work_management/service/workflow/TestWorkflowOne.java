package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.workflow.BaseWorkflow;
import edu.stanford.slac.core_work_management.model.workflow.Workflow;
import edu.stanford.slac.core_work_management.model.workflow.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Workflow(
        name = "TestWorkflowOne",
        description = "The workflow for a test"
)
@Component("TestWorkflowOne")
public class TestWorkflowOne extends BaseWorkflow {
    public TestWorkflowOne() {
        validTransitions = Map.of(
                // Rule: workStatus = "Approved"
                WorkflowState.Submitted, Set.of(WorkflowState.Approved),
                // Rule: becomes active when startDate starts
                WorkflowState.Approved, Set.of(WorkflowState.InProgress),
                // Rule: becomes active when endDate reaches or user tag completed the work
                WorkflowState.InProgress, Set.of(WorkflowState.Closed)
        );
    }
    @Override
    public void updateWithModel(Work work) {

    }
}
