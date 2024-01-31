package edu.stanford.slac.core_work_management.api.v1.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

@Documented
@Constraint(validatedBy = GroupOfFieldNotEmptyValidator.class)
@Target({FIELD, METHOD, PARAMETER, TYPE, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateGroupOfFieldNotEmpty {
    String message() default "Invalid fields. Either externalLocationIdentifier should be not null or name and description should be not empty.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] fieldsToCheck() default {};
    String[] againstFields() default {};
}
