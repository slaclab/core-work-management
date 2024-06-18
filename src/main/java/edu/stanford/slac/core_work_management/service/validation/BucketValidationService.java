package edu.stanford.slac.core_work_management.service.validation;

import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.value.LOVField;
import edu.stanford.slac.core_work_management.repository.LOVElementRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Field;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@Validated
@AllArgsConstructor
public class BucketValidationService {
    LOVElementRepository lovElementRepository;
    public boolean verify(@NotNull BucketSlot source) {
        for (Field field : source.getClass().getDeclaredFields()) {
            LOVField annotation = field.getAnnotation(LOVField.class);
            if (annotation != null) {
                validateField(source, field, annotation);
            }
        }
        return true;
    }

    private void validateField(Object source, Field field, LOVField annotationConstraint) {
        field.setAccessible(true);
        try {
            Object value = field.get(source);
            if (value == null && annotationConstraint.isMandatory()) {
                throw new IllegalArgumentException("The field %s is mandatory and need a LOV value".formatted(field.getName()));
            } else if (
                // in case the value is present
                    value != null
                            // check if the value can be accepted
                            && !wrapCatch(
                            () -> lovElementRepository.existsByIdAndFieldReferenceContains
                                    (
                                            value.toString(),
                                            annotationConstraint.fieldReference()
                                    ),
                            -1
                    )
            ) {
                throw new IllegalArgumentException("Invalid value '%s 'for LOV-validated field '%s'".formatted(value, field.getName()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field during LOV validation", e);
        }
    }
}
