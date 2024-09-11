package validation

import edu.stanford.slac.core_work_management.exception.WorkflowNotManuallyUpdatable
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.NewWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.UpdateWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.WorkflowWorkUpdate
import edu.stanford.slac.core_work_management.service.workflow.WorkflowState
class DummyChildValidation extends WorkTypeValidation {
    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workType = workflowWorkUpdate.getWorkType();
        var workflow = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();
        if (work == null) return;
        if (updateWorkflowState != null && work.getCurrentStatus().getStatus() != WorkflowState.Created) {
            throw WorkflowNotManuallyUpdatable.of()
                    .errorCode(-1)
                    .build();
        }
        // return if it is closed
        if (work.getCurrentStatus().getStatus() == WorkflowState.Closed) return;

        if (updateWorkflowState == null) return;
        workflow.moveToState(work, updateWorkflowState);
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        println "DummyChildValidation checkValid"
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        println "DummyChildValidation checkValid"
    }
}
