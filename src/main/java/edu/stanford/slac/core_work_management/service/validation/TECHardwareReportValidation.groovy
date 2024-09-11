package edu.stanford.slac.core_work_management.service.validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.model.WorkType
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.ShopGroupService
import edu.stanford.slac.core_work_management.service.workflow.BaseWorkflow
import edu.stanford.slac.core_work_management.service.workflow.NewWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.ReportWorkflow
import edu.stanford.slac.core_work_management.service.workflow.UpdateWorkValidation
import edu.stanford.slac.core_work_management.service.workflow.WorkflowState
import edu.stanford.slac.core_work_management.service.workflow.WorkflowWorkUpdate
import jakarta.validation.ConstraintValidatorContext
import lombok.AllArgsConstructor
import lombok.extern.log4j.Log4j2

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion

/**
 * Validation for the TECHardwareReport work type.
 */
@Log4j2
@AllArgsConstructor
class TECHardwareReportValidation extends WorkTypeValidation {
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workType = workflowWorkUpdate.getWorkType();
        var workflow = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();
        if (!ReportWorkflow.isAssignableFrom(workflow)) {
            throw ControllerLogicException.builder()
                    .errorCode(-1)
                    .errorMessage("Invalid workflow type")
                    .errorDomain("TECHardwareReportValidation::update")
                    .build()
        }

        // assigned to can be empty only in created state
        checkAssignedTo(work);

        // get current state
        var currentStatus = work.getCurrentStatus().getStatus();
        switch (currentStatus) {
            case Created -> {
                if (work.getAssignedTo() != null) {
                    workflow.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Assigned).build());
                }
            }
            case Submitted -> {
            }
            case PendingAssignment -> {
            }
            case Assigned -> {
//                var subsystemAttribute = checkFiledPresence(
//                        work.getDomainId().getCustomFields(),
//                        newWorkValidation.getNewWorkDTO().customFieldValues(),
//                        "radiationControlForm",
//                        context);
            }
            case ReadyForWork -> {
            }
            case InProgress -> {
            }
            case PendingApproval -> {
            }
            case PendingPaperwork -> {
            }
            case Approved -> {
            }
            case WorkComplete -> {
            }
            case ReviewToClose -> {
            }
            case Closed -> {
            }
            case None -> {
            }
        }
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        def validationResults = [
                checkStringField(newWorkValidation.newWorkDTO.title(), "title"),
                checkStringField(newWorkValidation.newWorkDTO.description(), "description"),
                checkStringField(newWorkValidation.newWorkDTO.locationId(), "locationId"),
                checkStringField(newWorkValidation.newWorkDTO.shopGroupId(), "shopGroupId"),
                checkFiledPresence(newWorkValidation.workType.customFields, newWorkValidation.newWorkDTO.customFieldValues(), "subsystem"),
                checkFiledPresence(newWorkValidation.workType.customFields, newWorkValidation.newWorkDTO.customFieldValues(), "group"),
                checkFiledPresence(newWorkValidation.workType.customFields, newWorkValidation.newWorkDTO.customFieldValues(), "urgency")
        ]

// Check if any validation failed
        def hasErrors = validationResults.any { !it.isValid() }

// Collect error messages if any validation failed
        def errorMessages = validationResults.findAll { !it.isValid() }
                .collect { it.errorMessage }

// If there are errors, throw a ControllerLogicException with all error messages
        if (hasErrors) {
            def allErrors = errorMessages.join(", ")
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage(allErrors)
                    .errorDomain("TECHardwareReportValidation::checkValid")
                    .build()
        }
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {

    }

    /**
     * Check if the string field is not null or empty
     *
     * @param work the work to check
     * @throws ControllerLogicException if the field is null or empty or with unalloyed user
     */
    private void checkAssignedTo(Work work) {
        // the assignedTo can be null or empty only if we are in created state
        if (work.getCurrentStatus().getStatus() != WorkflowState.Created && (work.getAssignedTo() == null || work.getAssignedTo().isEmpty())) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The assignedTo field is required in the current state")
                    .errorDomain("ReportWorkflow::checkAssignedTo")
                    .build();
        }

        for (String user : work.getAssignedTo()) {
            assertion(
                    () -> shopGroupService.checkContainsAUserEmail(work.getDomainId(), work.getShopGroupId(), user),
                    ControllerLogicException
                            .builder()
                            .errorCode(-3)
                            .errorMessage("The user is not part of the shop group")
                            .errorDomain("WorkService::update")
                            .build()
            );
        }
    }

}
