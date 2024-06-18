package edu.stanford.slac.core_work_management.api.v1.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class ValidDateValidator implements ConstraintValidator<ValidDate, LocalDateTime> {

    @Override
    public void initialize(ValidDate constraintAnnotation) {
        // Initialization code if necessary
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return false; // Consider null values as valid. Use @NotNull for null checks.
        }

        // Custom validation logic
        return true;
    }
}

