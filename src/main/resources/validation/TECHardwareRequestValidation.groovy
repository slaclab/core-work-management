package validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService
import edu.stanford.slac.core_work_management.model.CustomField
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.model.value.BooleanValue
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.ShopGroupService
import edu.stanford.slac.core_work_management.service.validation.ValidationResult
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.*
import org.springframework.beans.factory.annotation.Autowired

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion

/**
 * Validation for the TECHardwareReport work type.
 */
class TECHardwareRequestValidation extends WorkTypeValidation {
    @Autowired
    private final WorkRepository workRepository;
    @Autowired
    private final PeopleGroupService peopleGroupService;
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
                if (work.getCurrentBucketAssociation() != null && work.getCurrentBucketAssociation().getBucket() != null) {
                    // attempt to move to the next state
                    workflow.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.PendingApproval).build());
                }
            }
            case PendingApproval -> {
                if (updateWorkflowState.getNewState() == WorkflowState.ReadyForWork) {
                    // if all mandatory field have been filled
                    // the work can be approved
                    canMoveToReadyForWork(work);
                    // attempt to move to the next state
                    workflow.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReadyForWork).build());
                }
            }
            case ReadyForWork -> {
            }
            case InProgress -> {

            }
            case WorkComplete -> {
            }
            case Closed -> {
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
        ]

// Check if any validation failed
        def hasErrors = validationResults.any { !it.isValid() }

// If there are errors, throw a ControllerLogicException with all error messages
        if (hasErrors) {
            def errorMessages = validationResults.findAll { !it.isValid() }
                    .collect { it.errorMessage }
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage( errorMessages.join(", "))
                    .errorDomain("TECHardwareReportValidation::checkValid")
                    .build()
        }
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {}

    @Override
    void admitChildren(AdmitChildrenValidation canHaveChildValidation) {}

    /**
     * Check if the work can be moved to the ReadyForWork state
     *
     * @param work the work to check
     * @throws ControllerLogicException if the work cannot be moved to the ReadyForWork state
     */
    private void canMoveToReadyForWork(Work work) {
        ValidationResult<CustomField> radiationSafetyWorkControlForm = null;
        def validationResults = [
                checkAssignedTo(work),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "schedulingPriority"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "plannedStartDateTime"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "accessRequirements"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "ppsZone"),
                radiationSafetyWorkControlForm = checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "radiationSafetyWorkControlForm"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "lockAndTag"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "subsystem"),
                checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "group"),

        ]

        // in case the safety form is needed i need to check if the user has uploaded the file
        if(radiationSafetyWorkControlForm.valid && (radiationSafetyWorkControlForm.payload.value as BooleanValue).value) {
            // check for safety attachment
            ValidationResult<CustomField> rswcfAttachments = null;
            validationResults.add(rswcfAttachments = checkWorkFiledPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "rswcfAttachments"));
            if(rswcfAttachments.valid && rswcfAttachments.payload.getValue() == null) {
                validationResults.add(ValidationResult.failure("The Radiation Safety Work Control Form attachment is required"));
            }
        }

        // check if we have some errors
        def hasErrors = validationResults.any { !it.isValid() }
        if (hasErrors) {
            // collect all error messages
            def errorMessages = validationResults.findAll { !it.isValid() }
                    .collect { it.errorMessage }
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage(errorMessages.join(", "))
                    .errorDomain("TECHardwareReportValidation::canMoveToReadyForWork")
                    .build()
        }
    }

/**
 * Check if the string field is not null or empty
 *
 * @param work the work to check
 * @throws ControllerLogicException if the field is null or empty or with unalloyed user
 */
    private List<ValidationResult<String>> checkAssignedTo(Work work) {
        List<ValidationResult<String>> result = new ArrayList<>();
        def assignedUsers = work.getAssignedTo() ?: []
        // the assignedTo can be null or empty only if we are in created state
        assignedUsers.each { user ->
            try {
                peopleGroupService.findPersonByEMail(user)
            } catch (PersonNotFound e) {
                result.add(ValidationResult.failure("The user '${user}' does not exist"))
            }
        }
        return result.isEmpty() ? [ValidationResult.success("assignedTo")] : result
    }

}
