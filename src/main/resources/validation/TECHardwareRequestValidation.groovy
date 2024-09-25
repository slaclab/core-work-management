package validation

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException
import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService
import edu.stanford.slac.core_work_management.model.CustomField
import edu.stanford.slac.core_work_management.model.EventTrigger
import edu.stanford.slac.core_work_management.model.ProcessWorkflowInfo
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState
import edu.stanford.slac.core_work_management.model.Work
import edu.stanford.slac.core_work_management.model.value.AttachmentsValue
import edu.stanford.slac.core_work_management.model.value.BooleanValue
import edu.stanford.slac.core_work_management.model.value.DateTimeValue
import edu.stanford.slac.core_work_management.repository.EventTriggerRepository
import edu.stanford.slac.core_work_management.repository.WorkRepository
import edu.stanford.slac.core_work_management.service.AttachmentService
import edu.stanford.slac.core_work_management.service.BucketService
import edu.stanford.slac.core_work_management.service.validation.ValidationResult
import edu.stanford.slac.core_work_management.service.validation.WorkTypeValidation
import edu.stanford.slac.core_work_management.service.workflow.*

import java.time.LocalDateTime

/**
 * Validation for the TECHardwareReport work type.
 */
class TECHardwareRequestValidation extends WorkTypeValidation {
    private final WorkRepository workRepository;
    private final BucketService bucketService;
    private final AttachmentService attachmentService;
    private final PeopleGroupService peopleGroupService;
    private final EventTriggerRepository eventTriggerRepository;

    TECHardwareRequestValidation(WorkRepository workRepository, BucketService bucketService, AttachmentService attachmentService, PeopleGroupService peopleGroupService, EventTriggerRepository eventTriggerRepository) {
        this.workRepository = workRepository
        this.bucketService = bucketService
        this.attachmentService = attachmentService
        this.peopleGroupService = peopleGroupService
        this.eventTriggerRepository = eventTriggerRepository
    }

    @Override
    void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) {
        var work = workflowWorkUpdate.getWork();
        var workflowInstance = workflowWorkUpdate.getWorkflow();
        var updateWorkflowState = workflowWorkUpdate.getUpdateWorkflowState();
        if (!workflowInstance.getClass().isAssignableFrom(RequestWorkflow.class)) {
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
                var plannedStartDate = checkWorkFieldPresence(work, "plannedStartDateTime", Optional.empty());
                var bucketId = work.getCurrentBucketAssociation()?.getBucketId();

                if (plannedStartDate.valid || bucketId != null) {
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.PendingApproval).build());
                }
            }
            case WorkflowState.PendingApproval -> {
                if (updateWorkflowState.getNewState() == WorkflowState.ReadyForWork) {
                    // if all mandatory field have been filled
                    // the work can be approved
                    canMoveToReadyForWork(work);

                    // before we move to the next state we need to check if work is using a start planned date
                    var plannedStartDate = checkWorkFieldPresence(work, "plannedStartDateTime", Optional.empty());
                    if (plannedStartDate.valid && plannedStartDate.payload.getValue() != null) {
                        // we have a planned start date so i need to create a triggered event for it
                        var createdEventTrigger = eventTriggerRepository.save(
                                EventTrigger.builder()
                                        .referenceId(work.getId())
                                        .eventFireTimestamp((plannedStartDate.payload.getValue() as DateTimeValue).value)
                                        .typeName("workPlannedStart")
                                        .payload(
                                                ProcessWorkflowInfo.builder()
                                                        .domainId(work.getDomainId())
                                                        .workId(work.getId())
                                                        .build()
                                        )
                                        .build()
                        );
                    }

                    // attempt to move to the next state
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.ReadyForWork).build());
                }
            }
            case WorkflowState.ReadyForWork -> {
                // check if work has a planning date or a bucket
                LocalDateTime inProgressStarDate = null;
                boolean haveABucket = work.getCurrentBucketAssociation() != null && work.getCurrentBucketAssociation().getBucketId() != null;

                if (haveABucket) {
                    bucketService.findById(work.getCurrentBucketAssociation().getBucketId())
                            .ifPresent { bucket ->
                                inProgressStarDate = bucket.getStartDate();
                            }
                            .orElseThrow(() -> ControllerLogicException.builder()
                                    .errorCode(-1)
                                    .errorMessage("The bucket does not exist")
                                    .errorDomain("TECHardwareReportValidation::update")
                                    .build());
                } else {
                    var plannedStartDate = checkWorkFieldPresence(work.getWorkType().getCustomFields(), work.getCustomFields(), "plannedStartDateTime", Optional.empty());
                    if (!plannedStartDate.valid || plannedStartDate.payload.getValue() == null) {
                        throw ControllerLogicException.builder()
                                .errorCode(-1)
                                .errorMessage("The planned start date is null")
                                .errorDomain("TECHardwareReportValidation::update")
                                .build();
                    }
                    inProgressStarDate = (plannedStartDate.payload.getValue() as DateTimeValue).value;
                }

                // check if current date is before the inProgressStarDate
                var now = LocalDateTime.now();
                if (now.isBefore(inProgressStarDate) || now.isEqual(inProgressStarDate)) {
                   // move to in progress
                    workflowInstance.moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.InProgress).build());
                }
            }
            case WorkflowState.InProgress -> {

            }
            case WorkflowState.WorkComplete -> {
            }
            case WorkflowState.Closed -> {
            }
        }
    }

    @Override
    void checkValid(NewWorkValidation newWorkValidation) {
        def validationResults = [
                checkStringField(newWorkValidation.newWorkDTO.getTitle(), "title", Optional.empty()),
                checkStringField(newWorkValidation.newWorkDTO.getDescription(), "description", Optional.empty()),
                checkObjectField(newWorkValidation.newWorkDTO.getLocation(), "location", Optional.of("The location is mandatory")),
                checkObjectField(newWorkValidation.newWorkDTO.getShopGroup(), "shopGroup", Optional.of("The shop group is mandatory")),
        ]
        checkAttachments(newWorkValidation.newWorkDTO, validationResults);
        checkPlannedDataAgainstBucket(newWorkValidation.newWorkDTO, validationResults)
        checkAndFireError(validationResults)
    }

    @Override
    void checkValid(UpdateWorkValidation updateWorkValidation) {
        def validationResults = []
        checkAttachments(updateWorkValidation.getExistingWork(), validationResults);
        checkPlannedDataAgainstBucket(updateWorkValidation.getExistingWork(), validationResults)
        checkAndFireError(validationResults)
    }

    @Override
    void admitChildren(AdmitChildrenValidation canHaveChildValidation) {
        throw ControllerLogicException.builder()
                .errorCode(-1)
                .errorMessage("The work type cannot have children")
                .errorDomain("TECHardwareReportValidation::admitChildren")
                .build()
    }

    /**
     * Check if the planned start date is present and if it is present the bucket should not be present
     * @param work the work to check
     * @param validationResults the list of validation results
     */
    private void checkPlannedDataAgainstBucket(Work work, ArrayList<ValidationResult<String>> validationResults) {
// check that or we have a planned data or bucket or none
        var plannedStartDate = checkWorkFieldPresence(work, "plannedStartDateTime", Optional.empty());
        var haveABucket = work.getCurrentBucketAssociation() != null && work.getCurrentBucketAssociation().getBucketId() != null;
        // verify that if we have a planned start date we don't have a bucket
        if (plannedStartDate.valid && haveABucket) {
            validationResults.add(ValidationResult.failure("The work cannot have a planned start date and a bucket at the same time"));
        }
        if (plannedStartDate.valid && plannedStartDate.payload == null) {
            validationResults.add(ValidationResult.failure("A valid planned start date is required"));
        }
    }

    /**
     * Validate the attachments
     * @param newWorkDTO the new work to validate
     * @param validationResults the list of validation results
     */
    private void checkAttachments(Work work, ArrayList<ValidationResult<String>> validationResults) {
        ValidationResult<CustomField> attachment = null;
        if ((attachment = checkWorkFieldPresence(
                work,
                "rswcfAttachments",
                Optional.empty()
        )).valid) {
            validateAttachment(attachment.payload.getValue() as AttachmentsValue, "RSWCF Attachments", validationResults)
        }

        if ((attachment = checkWorkFieldPresence(
                work,
                "attachmentsAndFiles",
                Optional.empty()
        )).valid) {
            validateAttachment(attachment.payload.getValue() as AttachmentsValue, "Attachments and Files", validationResults)
        }
    }

    /**
     * Check if the work can be moved to the ReadyForWork state
     *
     * @param work the work to check
     * @throws ControllerLogicException if the work cannot be moved to the ReadyForWork state
     */
    private void canMoveToReadyForWork(Work work) {
        ValidationResult<CustomField> radiationSafetyWorkControlForm = null;
        def validationResults = [
                checkWorkFieldPresence(work, "schedulingPriority", Optional.empty()),
                checkWorkFieldPresence(work, "plannedStartDateTime", Optional.empty()),
                checkWorkFieldPresence(work, "accessRequirements", Optional.empty()),
                checkWorkFieldPresence(work, "ppsZone", Optional.empty()),
                radiationSafetyWorkControlForm = checkWorkFieldPresence(work, "radiationSafetyWorkControlForm", Optional.empty()),
                checkWorkFieldPresence(work, "lockAndTag", Optional.empty()),
                checkWorkFieldPresence(work, "subsystem", Optional.empty()),
//                checkWorkFiledPresence(work, "group", Optional.empty()),

        ]

        checkPlannedDataAgainstBucket(work, validationResults)

        // in case the safety form is needed i need to check if the user has uploaded the file
        if (radiationSafetyWorkControlForm.valid && (radiationSafetyWorkControlForm.payload.getValue() as BooleanValue).value) {
            // check for safety attachment
            ValidationResult<CustomField> rswcfAttachments = null;
            validationResults.add(rswcfAttachments = checkWorkFieldPresence(work, "rswcfAttachments", Optional.empty()));
            if (rswcfAttachments.valid && rswcfAttachments.payload.getValue() == null) {
                validationResults.add(ValidationResult.failure("The Radiation Safety Work Control Form attachment is required"));
            }
        }

        // check if the work has been assigned to someone
        checkAssignedTo(work, validationResults);

        // check if we have some errors
        checkAndFireError(validationResults)
    }

/**
 * Check if the string field is not null or empty
 *
 * @param work the work to check
 * @throws ControllerLogicException if the field is null or empty or with unalloyed user
 */
    private void checkAssignedTo(Work work, ArrayList<ValidationResult<String>> validationResults) {
        def assignedUsers = work.getAssignedTo() ?: []
        if(assignedUsers.size()==0) {
            validationResults.add(ValidationResult.failure("The work must be assigned to someone"))
            return;
        }
        // the assignedTo can be null or empty only if we are in created state
        assignedUsers.each { user ->
            try {
                peopleGroupService.findPersonByEMail(user)
            } catch (PersonNotFound e) {
                validationResults.add(ValidationResult.failure("The user '${user}' does not exist"))
            }
        }
    }

    /**
     * Check if the string field is not null or empty
     * @param validationResults
     */
    private void checkAndFireError(ArrayList<ValidationResult<String>> validationResults) {
// Check if any validation failed
        def hasErrors = validationResults.any { !it.valid}

// If there are errors, throw a ControllerLogicException with all error messages
        if (hasErrors) {
            def errorMessages = validationResults.findAll { !it.valid }
                    .collect { it.errorMessage }
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage(errorMessages.join(", "))
                    .errorDomain("TECHardwareReportValidation::checkValid")
                    .build()
        }
    }

    /**
     * Validate the attachments
     * @param attachmentsValue the attachments to validate
     * @param errorMessage the error message to show in case of error
     */
    private void validateAttachment(AttachmentsValue attachmentsValue, String fieldName, List<ValidationResult<String>> validationResult) {
        if (attachmentsValue == null || attachmentsValue.getValue() == null || attachmentsValue.getValue().isEmpty()) {
            return;
        }
        attachmentsValue.getValue().forEach { attachmentId ->
            if(!attachmentService.exists(attachmentId)){
                validationResult.addFirst(ValidationResult.failure("The '%s' attachment %s does not exist".formatted(fieldName, attachmentId)))
            }
        }
    }
}
