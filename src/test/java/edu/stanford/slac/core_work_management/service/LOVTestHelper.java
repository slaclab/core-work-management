/*
 * -----------------------------------------------------------------------------
 * Title      : LOVTestHelper
 * ----------------------------------------------------------------------------
 * File       : LOVTestHelper.java
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

package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ValueDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ValueTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WriteCustomFieldDTO;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.Math.abs;
@Service
public class LOVTestHelper {
    LOVService lovService;

    public static LOVTestHelper getInstance(LOVService lovService) {
       return new LOVTestHelper(lovService);
    }

    public LOVTestHelper(LOVService lovService) {
        this.lovService = lovService;
    }

    /**
     * Generate random custom field values
     *
     * @param WATypeCustomFieldDTOS list of custom fields
     * @return custom field values
     */
    public List<WriteCustomFieldDTO> generateRandomCustomFieldValues(List<WATypeCustomField> WATypeCustomFieldDTOS){
        List<WriteCustomFieldDTO> results = new java.util.ArrayList<>();
        WATypeCustomFieldDTOS.forEach(
                customFieldDTO -> {
                    List<LOVElementDTO> listOfLOVValues = new java.util.ArrayList<>();
                    boolean isLov = lovService.checkIfFieldReferenceIsInUse(customFieldDTO.getLovFieldReference());
                    if (isLov) {
                        listOfLOVValues = lovService.findAllByFieldReference(customFieldDTO.getLovFieldReference());
                    }
                    switch (customFieldDTO.getValueType()) {
                        case String -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(customFieldDTO.getId())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomString(listOfLOVValues, 10))
                                                        .type(ValueTypeDTO.String)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Number -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(customFieldDTO.getId())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomNumber())
                                                        .type(ValueTypeDTO.Number)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Date -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(customFieldDTO.getId())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomDate())
                                                        .type(ValueTypeDTO.Date)
                                                        .build()
                                        )
                                        .build()
                        );
                        case DateTime -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(customFieldDTO.getId())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomDateTime())
                                                        .type(ValueTypeDTO.DateTime)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Boolean -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(customFieldDTO.getId())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomBoolean())
                                                        .type(ValueTypeDTO.Boolean)
                                                        .build()
                                        )
                                        .build()
                        );

                    }
                }
        );
        return results;
    }

    private String generateRandomBoolean() {
        return String.valueOf(java.util.UUID.randomUUID().toString().hashCode() % 2 == 0);
    }

    private String generateRandomDateTime() {
        return java.time.LocalDateTime.now().toString();
    }

    private String generateRandomDate() {
        return java.time.LocalDate.now().toString();
    }

    private String generateRandomNumber() {
        return String.valueOf(java.util.UUID.randomUUID().toString().hashCode());
    }

    private String generateRandomString(List<LOVElementDTO> listOfLOVValues, int i) {
        if (listOfLOVValues.isEmpty()) {
            //return random string of size i
            return java.util.UUID.randomUUID().toString().substring(0, i);
        } else {
            // return randomly one of the lov id (not value)
            return listOfLOVValues.get(abs(java.util.UUID.randomUUID().toString().hashCode() % listOfLOVValues.size())).id();
        }

    }
}
