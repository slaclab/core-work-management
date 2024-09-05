package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.core_work_management.exception.WorkflowNotManuallyUpdatable;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkStatusLog;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.stanford.slac.core_work_management.service.workflow.WorkflowState.*;

/**
 * This is a simple workflow for a prent work four states
 * Created -> InProgress -> ReviewToClose -> Closed
 * for testing purposes
 */
@Workflow(
        name = "TestWorkflowOne",
        description = "The workflow for a test"
)
@Component("DummyParentWorkflow")
public class DummyParentWorkflow extends BaseWorkflow {
    @Autowired
    WorkRepository workRepository;

    public DummyParentWorkflow(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    public DummyParentWorkflow() {
        validTransitions = Map.of(
                // Rule: work is created and if no children are present it can be closed by the user
                Created, Set.of(InProgress, Closed),
                // Rule: all children are closed
                InProgress, Set.of(WorkflowState.ReviewToClose),
                // Rule: admin user review and close the work
                WorkflowState.ReviewToClose, Set.of(Closed)
        );
    }

    /**
     * if works contains one or more children, it could be closed when
     * all the children are closed
     *
     * @param work                the work to update
     * @param updateWorkflowState the state to move to, this is optional and can be null
     */
    @Override
    public void update(Work work, UpdateWorkflowState updateWorkflowState) {
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

        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());

        // check if all the children are closed
        boolean allChildrenClosed = children.stream().allMatch(w -> w.getCurrentStatus().getStatus() == Closed);

        switch (work.getCurrentStatus().getStatus()) {
            case Created -> {
                if (children.isEmpty()) return;

                if (allChildrenClosed) {
                    // if there are children we can only move to the next state
                    moveToState(
                            work,
                            UpdateWorkflowState
                                    .builder()
                                    .newState(Closed)
                                    .build());
                } else {
                    // if there are children we can only move to the next state
                    moveToState(
                            work,
                            UpdateWorkflowState
                                    .builder()
                                    .newState(InProgress)
                                    .build());
                }
            }
            case ReviewToClose -> {
                moveToState(work, updateWorkflowState);
            }
            case InProgress -> {
                // here we need to check for the children
                if (!allChildrenClosed) return;
                // ok all the children are closed so we can move to the next state
                moveToState(
                        work,
                        UpdateWorkflowState
                                .builder()
                                .newState(ReviewToClose)
                                .build());
            }
            case Closed -> {
                // do nothing
            }
        }
    }

    @Override
    public void isValid(Work work) {

    }

    @Override
    public void canUpdate(String identityId, Work work) {
        // perform the base standard checks
        super.canUpdate(identityId, work);

    }

    @Override
    public boolean canCreateChild(Work work) {
        return isStatusEqualTo(work, Set.of(Created, InProgress));
    }

    @Override
    public boolean isCompleted(Work work) {
        if (work == null) return false;
        return work.getCurrentStatus().getStatus() == Closed;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return validTransitions.get(work.getCurrentStatus().getStatus());
    }
}
