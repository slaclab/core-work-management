/*
 * -----------------------------------------------------------------------------
 * Title      : ModelFieldValidation
 * ----------------------------------------------------------------------------
 * File       : ModelFieldValidation.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

/*
 * -----------------------------------------------------------------------------
 * Title      : LOVValidationListener
 * ----------------------------------------------------------------------------
 * File       : LOVValidationListener.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

/*
 * -----------------------------------------------------------------------------
 * Title      : LOVValidationListener
 * ----------------------------------------------------------------------------
 * File       : LOVValidationListener.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

/*
 * -----------------------------------------------------------------------------
 * Title      : LOVValidationListener
 * ----------------------------------------------------------------------------
 * File       : LOVValidationListener.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

package edu.stanford.slac.core_work_management.service.validation;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.CustomAttributeNotFound;
import edu.stanford.slac.core_work_management.exception.LOVValueNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.CustomField;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.value.*;
import edu.stanford.slac.core_work_management.repository.LOVElementRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static java.util.Collections.emptyList;

@Service
@Validated
@AllArgsConstructor
public class ModelFieldValidationService {
    LOVElementRepository lovElementRepository;

    public void verify(@NotNull Work work, @NotNull List<WATypeCustomField> customFields) {
        verify(work, Objects.requireNonNullElse(work.getCustomFields(),emptyList()), customFields);
    }
    public void verify(@NotNull Activity activity, @NotNull List<WATypeCustomField> customFields) {
        verify(activity, Objects.requireNonNullElse(activity.getCustomFields(),emptyList()), customFields);
    }
    public void verify(@NotNull Object source, @NotNull List<CustomField> customFieldValues, @NotNull List<WATypeCustomField> customFields) {
        // check duplicated id
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("There are duplicated custom field id")
                        .errorDomain("WorkService::validateCustomField")
                        .build(),
                () -> customFieldValues.stream()
                        // Group by the id
                        .collect(Collectors.groupingBy(CustomField::getId))
                        .values().stream()
                        // Filter groups having more than one element, indicating duplicates
                        .filter(duplicates -> duplicates.size() > 1)
                        .flatMap(Collection::stream)
                        .toList().isEmpty()
        );

        // check that all the id are valid
        customFieldValues.forEach(
                cv -> {
                    var foundField = customFields.stream().filter(cf -> cf.getId().compareTo(cv.getId()) == 0).findFirst();
                    // check if id is valid
                    assertion(
                            ControllerLogicException.builder()
                                    .errorCode(-2)
                                    .errorMessage("The field id %s has not been found".formatted(cv.getId()))
                                    .errorDomain("WorkService::validateCustomField")
                                    .build(),
                            foundField::isPresent
                    );

                    // check the type
                    assertion(
                            ControllerLogicException.builder()
                                    .errorCode(-3)
                                    .errorMessage("The field id %s has wrong type %s(%s)".formatted(cv.getId(), getType(cv.getValue()), foundField.get().getValueType()))
                                    .errorDomain("WorkService::validateCustomField")
                                    .build(),
                            () -> getType(cv.getValue()) == foundField.get().getValueType()
                    );
                }

        );

        // collect all the mandatory field
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-4)
                        .errorMessage("Not all mandatory attribute has been submitted")
                        .errorDomain("WorkService::validateCustomField")
                        .build(),
                () -> customFields
                        .stream()
                        .filter(WATypeCustomField::getIsMandatory)
                        .map(WATypeCustomField::getId)
                        .allMatch(
                                s -> customFieldValues.stream().anyMatch(av -> av.getId().compareTo(s) == 0)
                        )
        );


        // validate the static fields
        for (Field field : source.getClass().getDeclaredFields()) {
            LOVField annotation = field.getAnnotation(LOVField.class);
            if (annotation != null) {
                validateField(source, field, annotation);
            }
        }

        //validate the dynamic fields
        verifyDynamicField(customFieldValues, customFields);
    }

    private ValueType getType(AbstractValue value) {
        if(value.getClass().isAssignableFrom(BooleanValue.class)) {
            return ValueType.Boolean;
        } else if(value.getClass().isAssignableFrom(DateTimeValue.class)) {
            return ValueType.DateTime;
        } else if(value.getClass().isAssignableFrom(DateValue.class)) {
            return ValueType.Date;
        } else if(value.getClass().isAssignableFrom(NumberValue.class)) {
            return ValueType.Number;
        } else if(value.getClass().isAssignableFrom(DoubleValue.class)) {
            return ValueType.Double;
        } else if(value.getClass().isAssignableFrom(StringValue.class)) {
            return ValueType.String;
        } else {
            return null;
        }
    }

    /**
     * Verify the dynamic field
     * @param customFieldValues the custom field values
     * @param customFields the custom fields
     */
    private void verifyDynamicField(List<CustomField> customFieldValues, List<WATypeCustomField> customFields) {
        if(customFieldValues == null || customFields == null) return;
        //find relative field
        customFieldValues.forEach(
                customField -> {
                    WATypeCustomField waTypeCustomField = customFields.stream()
                            .filter(customField1 -> customField1.getId().equals(customField.getId()))
                            .findFirst()
                            .orElseThrow(
                                    () -> CustomAttributeNotFound.notFoundById()
                                            .id(customField.getId())
                                            .build()
                            );

                    boolean isLOV = lovElementRepository.existsByFieldReferenceContains(waTypeCustomField.getLovFieldReference());
                    if(!isLOV) return;

                    assertion(
                            ControllerLogicException.builder()
                                    .errorCode(-1)
                                    .errorMessage("The custom field %s need to use the string value".formatted(customField.getId()))
                                    .errorDomain("LOVValidation::verifyDynamicField")
                                    .build(),
                            ()->getType(customField.getValue())==ValueType.String
                    );

                    // check if the value is consistent with the list of possible values
                    String lovValueId = customField.getValue().toString();
                    assertion(
                            LOVValueNotFound.byId()
                                    .errorCode(-2)
                                    .id(lovValueId)
                                    .build(),
                            ()->lovElementRepository.existsByIdAndFieldReferenceContains
                                    (
                                            lovValueId,
                                            waTypeCustomField.getLovFieldReference()
                                    )
                    );
                });
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
