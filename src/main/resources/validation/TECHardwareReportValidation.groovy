package validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
import edu.stanford.slac.ad.eed.baselib.service.AuthService
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper
import edu.stanford.slac.core_work_management.exception.WorkflowDeniedAction
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.ShopGroupService
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.*
import groovy.util.logging.Slf4j

import java.time.Clock

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion

/**
 * Validation for the TECHardwareReport work type.
 */
@Slf4j
class TECHardwareReportValidation extends WorkTypeValidation {
    private final Clock clock;
    private final AuthService authService;
    private final DomainMapper domainMapper;
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    TECHardwareReportValidation(Clock clock, AuthService authService, DomainMapper domainMapper, WorkRepository workRepository, ShopGroupService shopGroupService) {
        this.clock = clock
        this.authService = authService
        this.domainMapper = domainMapper
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
        switch (currentStatus) {
            case WorkflowState.Created -> {
                manageInCreateState(workflowInstance, work)
            }
            case WorkflowState.Scheduled -> {
                manageScheduledState(workflowInstance, work)
            }
            case WorkflowState.InProgress -> {
                manageInProgressState(workflowInstance, work)
            }
            case WorkflowState.ReviewToClose -> {
                manageReviewToCodeState(workflowInstance, work, updateWorkflowState)
            }
            case WorkflowState.Closed -> {
            }
        }
    }


    private void manageReviewToCodeState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {
        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
        boolean childrenInReadyForWork = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.ReadyForWork);
        boolean childrenInProgress = children.stream().anyMatch(w -> EnumSet.of(WorkflowState.InProgress, WorkflowState.WorkComplete, WorkflowState.ReviewToClose).contains(w.getCurrentStatus().getStatus()));
        if (childrenInProgress) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
        } else if (childrenInReadyForWork) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Scheduled).build());
        } else if (updateWorkflowState != null) {
            workflowInstance.moveToState(work, updateWorkflowState);
        }
    }

    private void manageInProgressState(BaseWorkflow workflowInstance, Work work) {
        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
        boolean childrenInReadyForWork = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.ReadyForWork);
        boolean childrenInProgress = children.stream().anyMatch(w -> EnumSet.of(WorkflowState.InProgress, WorkflowState.WorkComplete, WorkflowState.ReviewToClose).contains(w.getCurrentStatus().getStatus()));
         if (!childrenInProgress && !childrenInReadyForWork) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReviewToClose).build());
        } else if (!childrenInProgress && childrenInReadyForWork) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Scheduled).build());
        }
    }

    private void manageScheduledState(BaseWorkflow workflowInstance, Work work) {
        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
        boolean childrenInReadyForWork = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.ReadyForWork);
        boolean childrenInProgress = children.stream().anyMatch(w -> EnumSet.of(WorkflowState.InProgress, WorkflowState.WorkComplete, WorkflowState.ReviewToClose).contains(w.getCurrentStatus().getStatus()));
        if (!childrenInProgress && !childrenInReadyForWork) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReviewToClose).build());
        } else if (childrenInProgress) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
        }
    }

    private void manageInCreateState(BaseWorkflow workflowInstance, Work work) {
        List<Work> children = workRepository.findByDomainIdAndParentWorkId(work.getDomainId(), work.getId());
        boolean childrenInReadyForWork = children.stream().anyMatch(w -> w.getCurrentStatus().getStatus() == WorkflowState.ReadyForWork);
        boolean childrenInProgress = children.stream().anyMatch(w -> EnumSet.of(WorkflowState.InProgress, WorkflowState.WorkComplete, WorkflowState.ReviewToClose).contains(w.getCurrentStatus().getStatus()));
        if (childrenInProgress) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
        } else if (childrenInReadyForWork) {
            workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Scheduled).build());
        }
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        def validationResults = [
                checkStringField(newWorkValidation.work.getTitle(), "title", Optional.empty()),
                checkStringField(newWorkValidation.work.getDescription(), "description", Optional.empty()),
                checkObjectField(newWorkValidation.work.getLocation(), "location", Optional.empty()),
                checkObjectField(newWorkValidation.work.getShopGroup(), "shopGroup", Optional.empty()),
                checkWorkFieldPresence(newWorkValidation.work, "project", Optional.empty()),
                checkWorkFieldPresence(newWorkValidation.work, "urgency", Optional.empty())
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        // check superclass validations
        super.checkValid(updateWorkValidation)
        def validationResults = [
                checkStringField(updateWorkValidation.getExistingWork().getTitle(), "title", Optional.empty()),
                checkStringField(updateWorkValidation.getExistingWork().getDescription(), "description", Optional.empty()),
                checkObjectField(updateWorkValidation.getExistingWork().getLocation(), "location", Optional.empty()),
                checkObjectField(updateWorkValidation.getExistingWork().getShopGroup(), "shopGroup", Optional.empty()),
                checkWorkFieldPresence(updateWorkValidation.getExistingWork(), "project", Optional.empty()),
                checkWorkFieldPresence(updateWorkValidation.getExistingWork(), "urgency", Optional.empty())
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void admitChildren(AdmitChildrenValidation canHaveChildValidation) {}

    @Override
    boolean isUserAuthorizedToUpdate(String userId, UpdateWorkValidation updateWorkValidation) {
        var work = updateWorkValidation.getExistingWork()
        var updateWorkDTO = updateWorkValidation.getUpdateWorkDTO()
        if(updateWorkDTO !=null && updateWorkDTO.workflowStateUpdate() != null) {
            var statusModelValue = domainMapper.toModel(updateWorkDTO.workflowStateUpdate())
            if(statusModelValue.getNewState() == WorkflowState.ReadyForWork) {
                // only area manage and root users can move to ready for work
                String areaManagerUserId = Objects.requireNonNull(work.getLocation()).getLocationManagerUserId()
                boolean isRoot = authService.checkForRoot(userId)
                if((areaManagerUserId==null || areaManagerUserId.compareToIgnoreCase(userId) != 0) && !isRoot) {
                    throw WorkflowDeniedAction.byErrorMessage()
                            .errorCode(-1)
                            .errorMessage("Only the area manager and root users can move the work to ReadyForWork")
                            .build()
                }
            }
        }
        return true;
    }
}
