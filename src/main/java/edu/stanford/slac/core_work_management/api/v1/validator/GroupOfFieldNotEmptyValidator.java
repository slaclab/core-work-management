package edu.stanford.slac.core_work_management.api.v1.validator;

import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.stream.Stream;

/**
 * Validator to check if a group of fields are not empty
 */
public class GroupOfFieldNotEmptyValidator implements ConstraintValidator<ValidateGroupOfFieldNotEmpty, Object> {
    private String[] fieldsToCheck;
    private String[] againstFields;

    @Override
    public void initialize(ValidateGroupOfFieldNotEmpty constraintAnnotation) {
        fieldsToCheck = constraintAnnotation.fieldsToCheck();
        againstFields = constraintAnnotation.againstFields();
    }

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context) {
        boolean filedToCheckAllNotEmpty = Stream.of(fieldsToCheck).allMatch(
                field -> checkFieldByString(dto, field)
        );
        boolean againstFieldsAllNotEmpty = Stream.of(againstFields).allMatch(
                field -> checkFieldByString(dto, field)
        );
        return (filedToCheckAllNotEmpty && !againstFieldsAllNotEmpty) ||
                (!filedToCheckAllNotEmpty && againstFieldsAllNotEmpty);

    }

    /**
     * Check if the field is not empty
     *
     * @param dto   the dto to check
     * @param field the field to check
     * @return true if the field is empty
     */
    private static boolean checkFieldByString(Object dto, String field) {
        Field declaredField = null;
        Object value = null;
        try {
            declaredField = dto.getClass().getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        declaredField.setAccessible(true);
        try {
            value = declaredField.get(dto);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return (value != null && !((String) value).isEmpty());
    }
}