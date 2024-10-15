package validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.ShopGroupService
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.*
import groovy.util.logging.Slf4j

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion

/**
 * Validation for the TECHardwareReport work type.
 */
@Slf4j
class TECHardwareReportValidation extends WorkTypeValidation {
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    TECHardwareReportValidation(WorkRepository workRepository, ShopGroupService shopGroupService) {
        this.workRepository = workRepository
        this.shopGroupService = shopGroupService
    }

    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workflowInstance = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();
        if (!workflowInstance.getClass().isAssignableFrom(ReportWorkflow.class)) {
            throw ControllerLogicException.builder()
                    .errorCode(-1)
                    .errorMessage("Invalid workflow type")
                    .errorDomain("TECHardwareReportValidation::update")
                    .build()
        }

        // get current state
        var currentStatus = work.getCurrentStatus().getStatus();
        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
        boolean childrenInReadyForWork = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.ReadyForWork);
        boolean childrenInProgress = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.InProgress);
        switch (currentStatus) {
            case WorkflowState.Created -> {
                if (childrenInProgress) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
                } else if (childrenInReadyForWork) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Scheduled).build());
                }
            }
            case WorkflowState.Scheduled -> {
                if (!childrenInProgress && !childrenInReadyForWork) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReviewToClose).build());
                } else if (childrenInReadyForWorks) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
                }
            }
            case WorkflowState.InProgress -> {
                if (!childrenInProgress && !childrenInReadyForWork) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReviewToClose).build());
                } else if (updateWorkflowState.getNewState() == WorkflowState.InProgress) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
                }
            }
            case WorkflowState.ReviewToClose -> {
                if (childrenInProgress) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
                } else if (childrenInReadyForWork) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Scheduled).build());
                } else if(updateWorkflowState!=null) {
                    workflowInstance.moveToState(work, updateWorkflowState);
                }
            }
            case WorkflowState.Closed -> {
            }
        }
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        def validationResults = [
                checkStringField(newWorkValidation.work.getTitle(), "title", Optional.empty()),
                checkStringField(newWorkValidation.work.getDescription(), "description", Optional.empty()),
                checkObjectField(newWorkValidation.work.getLocation(), "location", Optional.empty()),
                checkObjectField(newWorkValidation.work.getShopGroup(), "shopGroup", Optional.empty()),
                checkWorkFieldPresence(newWorkValidation.work, "group", Optional.empty()),
                checkWorkFieldPresence(newWorkValidation.work, "urgency", Optional.empty())
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        def validationResults = [
                checkStringField(updateWorkValidation.getExistingWork().getTitle(), "title", Optional.empty()),
                checkStringField(updateWorkValidation.getExistingWork().getDescription(), "description", Optional.empty()),
                checkObjectField(updateWorkValidation.getExistingWork().getLocation(), "location", Optional.empty()),
                checkObjectField(updateWorkValidation.getExistingWork().getShopGroup(), "shopGroup", Optional.empty()),
                checkWorkFieldPresence(updateWorkValidation.getExistingWork(), "group", Optional.empty()),
                checkWorkFieldPresence(updateWorkValidation.getExistingWork(), "urgency", Optional.empty())
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void admitChildren(AdmitChildrenValidation canHaveChildValidation) {}
}
