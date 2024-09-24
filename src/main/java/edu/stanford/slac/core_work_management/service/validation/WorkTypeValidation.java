package edu.stanford.slac.core_work_management.service.validation;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.WriteCustomFieldDTO;
import edu.stanford.slac.core_work_management.model.CustomField;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.service.workflow.AdmitChildrenValidation;
import edu.stanford.slac.core_work_management.service.workflow.NewWorkValidation;
import edu.stanford.slac.core_work_management.service.workflow.UpdateWorkValidation;
import edu.stanford.slac.core_work_management.service.workflow.WorkflowWorkUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Define the work type validation baseclass
 */
public abstract class WorkTypeValidation {
    /**
     * Update the workflow with the model for automatic transition
     * if a new state is provided it try to understand if the transition is valid
     * and in case new state is set
     *
     * @param workflowWorkUpdate the work's workflow update
     * @throws ControllerLogicException if the transition is not valid
     */
    abstract public void updateWorkflow(WorkflowWorkUpdate workflowWorkUpdate) throws ControllerLogicException;

    /**
     * Check if the work is valid
     * the validation is done according to the state of the workflow
     *
     * @param newWorkValidation the work validation info to check
     */
    abstract public void checkValid(NewWorkValidation newWorkValidation);

    /**
     * Check if the work update are valid
     * the validation is done according to the state of the workflow
     *
     * @param updateWorkValidation the work update information to check
     */
    abstract public void checkValid(UpdateWorkValidation updateWorkValidation);

    /**
     * Check if the current work can have child
     *
     * @param canHaveChildValidation the information to check if the work can have child
     */
    abstract public void admitChildren(AdmitChildrenValidation canHaveChildValidation);

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param customFields         the lis tof the custom field associated to the work type
     * @param writeCustomFieldList the user valorized custom field
     * @param customFieldName      the name of the custom field to check
     */
    protected ValidationResult<WriteCustomFieldDTO> checkFiledPresence(List<WATypeCustomField> customFields, List<WriteCustomFieldDTO> writeCustomFieldList, String customFieldName, Optional<String> error) {
        var filedToCheck = customFields.stream()
                .filter(customField -> customField.getName().compareToIgnoreCase(customFieldName) == 0)
                .findFirst();

        if (filedToCheck.isPresent() && writeCustomFieldList != null) {
            var customFieldFound = writeCustomFieldList.stream()
                    .filter(customField -> customField.id().compareTo(filedToCheck.get().getId()) == 0)
                    .findFirst();

            if (customFieldFound.isEmpty()) {
                // Return a failure validation result if the custom field is required but not found
                return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(customFieldName)));
            }

            // Return success if the custom field is found
            return ValidationResult.success(customFieldFound.get());
        }

        // Return failure if the field was not found in the list of custom fields
        return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(customFieldName)));
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param work the work to check
     */
    protected ValidationResult<CustomField> checkWorkFiledPresence(Work work , String customFieldName, Optional<String> error) {
        var filedToCheck = work.getWorkType().getCustomFields().stream()
                .filter(customField -> customField.getName().compareToIgnoreCase(customFieldName) == 0)
                .findFirst();

        if (filedToCheck.isPresent() && work.getCustomFields() != null) {
            var customFieldFound = work.getCustomFields().stream()
                    .filter(customField -> customField.getId().compareTo(filedToCheck.get().getId()) == 0)
                    .findFirst();

            if (customFieldFound.isEmpty()) {
                // Return a failure validation result if the custom field is required but not found
                return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(customFieldName)));
            }

            // Return success if the custom field is found
            return ValidationResult.success(customFieldFound.get());
        }

        // Return failure if the field was not found in the list of custom fields
        return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(customFieldName)));
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param fieldValue the value of the field to check
     * @param fieldName  the name of the field to check
     */
    protected ValidationResult<String> checkStringField(String fieldValue, String fieldName, Optional<String> error) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            // Return failure with an error message
            return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(fieldName)));
        }

        // Return success with the field name as payload
        return ValidationResult.success(fieldName);
    }

    /**
     * Check if the status of the work is equal to any provided states
     *
     * @param fieldValue the value of the field to check
     * @param fieldName  the name of the field to check
     */
    protected ValidationResult<String> checkObjectField(Object fieldValue, String fieldName, Optional<String> error) {
        if (fieldValue == null) {
            // Return failure with an error message
            return ValidationResult.failure(error.orElse("The field '%s' is required".formatted(fieldName)));
        }

        // Return success with the field name as payload
        return ValidationResult.success(fieldName);
    }
}
