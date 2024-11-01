package validation

import edu.stanford.slac.core_work_management.exception.WorkCannotHaveChildren
import edu.stanford.slac.core_work_management.exception.WorkflowNotManuallyUpdatable
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.AdmitChildrenValidation
import edu.stanford.slac.core_work_management.service.workflow.NewWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.UpdateWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.WorkflowWorkUpdate
import org.springframework.beans.factory.annotation.Autowired

import static edu.stanford.slac.core_work_management.service.workflow.WorkflowState.*

class DummyParentValidation extends WorkTypeValidation{
    @Autowired
    WorkRepository workRepository

    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workType = workflowWorkUpdate.getWork().getWorkType();
        var workflow = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();
        if (work == null) return;
        // check when is possible to force the state:
        if (updateWorkflowState != null && updateWorkflowState.getNewState() != null && work.getCurrentStatus().getStatus() != ReviewToClose) {
            throw WorkflowNotManuallyUpdatable.of()
                    .errorCode(-1)
                    .build();
        }
        // return if it is closed
        if (work.getCurrentStatus().getStatus() == Closed) {
            return;
        }

        switch (work.currentStatus?.status) {
            case Created:
                if(work.getId()==null) break;
                List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
                // check if all the children are closed
                boolean allChildrenClosed = children.stream().allMatch(w -> w.getCurrentStatus().getStatus() == Closed);
                if (children.isEmpty()) return

                if (allChildrenClosed) {
                    workflow.moveToState(work, UpdateWorkflowState.builder().newState(Closed).build())
                } else {
                    workflow.moveToState(work, UpdateWorkflowState.builder().newState(InProgress).build())
                }
                break

            case ReviewToClose:
                workflow.moveToState(work, updateWorkflowState)
                break

            case InProgress:
                List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
                // check if all the children are closed
                boolean allChildrenClosed = children.stream().allMatch(w -> w.getCurrentStatus().getStatus() == Closed);
                if (!allChildrenClosed) return
                workflow.moveToState(work, UpdateWorkflowState.builder().newState(ReviewToClose).build())
                break

            case Closed:
                // Do nothing
                break
        }
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        println "DummyParentValidation checkValid"
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        println "DummyParentValidation checkValid"
    }

    @Override
    void admitChildren(AdmitChildrenValidation admitChildrenValidation) {
        var work = admitChildrenValidation.getWork();
        var workType = admitChildrenValidation.getWorkType();
        var workflow = admitChildrenValidation.getWorkflow();
        if (!workflow.isStatusEqualTo(work, Set.of(Created, InProgress))) {
            throw WorkCannotHaveChildren.builder()
                    .errorCode(-1)
                    .errorMessage("Work cannot have children")
                    .errorDomain("DummyParentValidation")
                    .build();
        }
    }
}
