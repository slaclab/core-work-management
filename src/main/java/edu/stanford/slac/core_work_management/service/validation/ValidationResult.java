package edu.stanford.slac.core_work_management.service.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ValidationResult<T> {
    private final boolean isValid;
    private final String errorMessage;
    private final T payload;

    static public ValidationResult<Object> success() {
        return ValidationResult.builder().isValid(true).build();
    }

    static public <T> ValidationResult<T> success(T payload) {
        return ValidationResult.<T>builder().isValid(true).payload(payload).build();
    }

    static public <T> ValidationResult<T> failure(String errorMessage) {
        return ValidationResult.<T>builder().isValid(false).errorMessage(errorMessage).build();
    }
}