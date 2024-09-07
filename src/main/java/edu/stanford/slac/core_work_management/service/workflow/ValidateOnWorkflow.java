package edu.stanford.slac.core_work_management.service.workflow;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {WorkflowValidator.class})
public @interface ValidateOnWorkflow {
    String message() default "Invalid data for the workflow";
    // Required groups and payload attributes
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
