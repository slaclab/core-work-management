package validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
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

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion

/**
 * Validation for the TECHardwareReport work type.
 */
@Slf4j
class TECSoftwareReportValidation extends WorkTypeValidation {
    private final DomainMapper domainMapper;
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    TECSoftwareReportValidation(DomainMapper domainMapper, WorkRepository workRepository, ShopGroupService shopGroupService) {
        this.domainMapper = domainMapper
        this.workRepository = workRepository
        this.shopGroupService = shopGroupService
    }

    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workflowInstance = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();

        // get current state
        var currentStatus = work.getCurrentStatus().getStatus();
        switch (currentStatus) {
            case WorkflowState.Created -> {
                manageInCreateState(workflowInstance, work, updateWorkflowState)
            }
            case WorkflowState.Approved -> {
                manageApprovedState(workflowInstance, work, updateWorkflowState)
            }
            case WorkflowState.InProgress -> {
                manageInProgressState(workflowInstance, work, updateWorkflowState)
            }
            case WorkflowState.Closed -> {
            }
        }
    }

    private void manageInProgressState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {

    }

    private void manageApprovedState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {

    }

    private void manageInCreateState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {

    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        def validationResults = [
                checkStringField(newWorkValidation.work.getTitle(), "title", Optional.empty()),
                checkStringField(newWorkValidation.work.getDescription(), "description", Optional.empty()),
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        def validationResults = [
                checkStringField(updateWorkValidation.getExistingWork().getTitle(), "title", Optional.empty()),
                checkStringField(updateWorkValidation.getExistingWork().getDescription(), "description", Optional.empty()),
        ]
        checkAndFireError(validationResults)
    }

    @Override
    void admitChildren(AdmitChildrenValidation canHaveChildValidation) {}

    @Override
    boolean isUserAuthorizedToUpdate(String userId, WorkDTO workDTO, UpdateWorkDTO updateWorkDTO) {
        if(updateWorkDTO !=null && updateWorkDTO.workflowStateUpdate() != null) {
            var statusModelValue = domainMapper.toModel(updateWorkDTO.workflowStateUpdate())
            if(statusModelValue.getNewState() == WorkflowState.ReadyForWork) {
                // only area manage and root users can move to ready for work
                String areaManagerUserId = Objects.requireNonNull(workDTO.location()).locationManagerUserId()
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
