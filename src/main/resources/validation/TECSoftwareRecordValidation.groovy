package validation

import edu.stanford.slac.core_work_management.api.v1.dto.BucketSlotDTO
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper
import edu.stanford.slac.core_work_management.exception.WorkflowDeniedAction
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.ShopGroupService
import edu.stanford.slac.core_work_management.service.validation.ValidationResult
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.*
import groovy.util.logging.Slf4j
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

import java.time.Clock
import java.time.LocalDateTime

/**
 * Validation for the TECHardwareReport work type.
 */
@Slf4j
class TECSoftwareReportValidation extends WorkTypeValidation {
    private final Clock clock;
    private final DomainMapper domainMapper;
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    TECSoftwareReportValidation(Clock clock, DomainMapper domainMapper, WorkRepository workRepository, ShopGroupService shopGroupService) {
        this.clock = clock
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
            case WorkflowState.InProgress -> {
                manageInProgressState(workflowInstance, work, updateWorkflowState)
            }
            case WorkflowState.Closed -> {
            }
        }
    }

    /**
     * Manage the work when is in the in progress state
     * @param workflowInstance the workflow instance
     * @param work the work
     * @param updateWorkflowState the update workflow state
     */
    private void manageInProgressState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {
        // check if user what close the record upon creation
        if(updateWorkflowState!= null && updateWorkflowState.getNewState() == WorkflowState.Closed) {
            workflowInstance.moveToState(work, updateWorkflowState);
        }
    }

    /**
     * Manage the work when is in the created state
     * @param workflowInstance the workflow instance
     * @param work the work
     * @param updateWorkflowState the update workflow state
     */
    private void manageInCreateState(BaseWorkflow workflowInstance, Work work, UpdateWorkflowState updateWorkflowState) {
        // check if assigned is null assign the creator
        if(work.getAssignedTo() == null || work.getAssignedTo().isEmpty()) {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            work.setAssignedTo(List.of(auth.getPrincipal().toString()))
        }

        // check if user what close the record upon creation
        if(updateWorkflowState!= null && updateWorkflowState.getNewState() == WorkflowState.Closed) {
            workflowInstance.moveToState(work, updateWorkflowState);
        }

        // check if are associated to an bucket so if we need to transition to in progress
        boolean haveABucket = work.getCurrentBucketAssociation() != null && work.getCurrentBucketAssociation().getBucketId() != null;
        if (haveABucket) {
            BucketSlotDTO bucket = bucketService.findById(work.getCurrentBucketAssociation().getBucketId())
            LocalDateTime inProgressStarDate = bucket.from();
            var now = LocalDateTime.now(clock);
            if (now.isAfter(inProgressStarDate) || now.isEqual(inProgressStarDate)) {
                // move to in progress
                workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
            }
        }
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
        // check superclass validations
        super.checkValid(updateWorkValidation);
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
