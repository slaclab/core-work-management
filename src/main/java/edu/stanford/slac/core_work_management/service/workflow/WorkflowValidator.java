package edu.stanford.slac.core_work_management.service.workflow;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WorkflowValidator implements ConstraintValidator<ValidateOnWorkflow,  WorkflowValidation<?>> {
    @Override
    public void initialize(ValidateOnWorkflow constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(WorkflowValidation<?> toValidate, ConstraintValidatorContext context) {
        boolean isValid = false;
        if (toValidate.getValue() instanceof NewWorkValidation newWorkValidation) {
            isValid = toValidate.getWorkflow().isValid(newWorkValidation, context);
        } else if (toValidate.getValue() instanceof UpdateWorkValidation updateWorkValidation) {
            isValid = toValidate.getWorkflow().isValid(updateWorkValidation, context);
        }
        return isValid;
    }
}
