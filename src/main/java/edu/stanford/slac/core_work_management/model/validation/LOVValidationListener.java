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

package edu.stanford.slac.core_work_management.model.validation;

import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.LOVDomainType;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.value.LOVDomain;
import edu.stanford.slac.core_work_management.model.value.LOVField;
import edu.stanford.slac.core_work_management.repository.LOVElementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Component
public class LOVValidationListener implements ApplicationListener<BeforeConvertEvent<Object>> {

    @Autowired
    private LOVElementRepository lovElementRepository;

    @Override
    public void onApplicationEvent(BeforeConvertEvent<Object> event) {
        if (!event.getSource().getClass().isAssignableFrom(Work.class) &&
                !event.getSource().getClass().isAssignableFrom(Activity.class)) {
            return;
        }
        Object source = event.getSource();
        if (!source.getClass().isAnnotationPresent(LOVDomain.class)) {
            return;
        }
        LOVDomain domain = source.getClass().getAnnotation(LOVDomain.class);
        for (Field field : source.getClass().getDeclaredFields()) {
            LOVField annotation = field.getAnnotation(LOVField.class);
            if (annotation != null) {
                validateField(source, field, domain, annotation);
            }
        }
    }

    private void validateField(Object source, Field field, LOVDomain domainConstraint, LOVField annotationConstraint) {
        field.setAccessible(true);
        try {
            Object value = field.get(source);
            if (
                    // in case the value is present
                    value != null
                            // check if the value can be accepted
                            && !wrapCatch(
                            () -> lovElementRepository.existsByIdAndDomainAndFieldReferenceContains
                                    (
                                            value.toString(),
                                            domainConstraint.value(),
                                            annotationConstraint.fieldReference()
                                    ),
                            -1
                    )
            ) {
                throw new IllegalArgumentException("Invalid value for LOV-validated field");
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field during LOV validation", e);
        }
    }
}
