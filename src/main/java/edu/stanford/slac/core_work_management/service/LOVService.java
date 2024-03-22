/*
 * -----------------------------------------------------------------------------
 * Title      : LOVService
 * ----------------------------------------------------------------------------
 * File       : LOVService.java
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

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.LOVMapper;
import edu.stanford.slac.core_work_management.exception.LOVValueNotFound;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.model.value.AbstractValue;
import edu.stanford.slac.core_work_management.model.value.LOVField;
import edu.stanford.slac.core_work_management.exception.LOVFieldReferenceNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.LOVElementRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@Validated
@AllArgsConstructor
public class LOVService {
    private final LOVMapper lovMapper;
    private final ActivityTypeRepository activityTypeRepository;
    private final LOVElementRepository lovElementRepository;

    /**
     * Create a new LOV element
     *
     * @param lovDomainDTO   the domain of the LOV element
     * @param fieldName      used to find the field reference where the LOV values will be associated
     * @param lovElementDTOs the list new LOV element
     * @return the id of the new LOV element
     */
    public List<String> createNew(@NotNull LOVDomainTypeDTO lovDomainDTO, @NotEmpty String fieldName, @Valid List<NewLOVElementDTO> lovElementDTOs) {
        var fieldReferences = getLOVFieldReference(lovDomainDTO);
        assertion(
                LOVFieldReferenceNotFound.byFieldName().errorCode(-1).fieldName(fieldName).build(),
                () -> fieldReferences.containsKey(fieldName)
        );
        return lovElementDTOs.stream()
                .map(e -> lovMapper.toModel(lovDomainDTO, fieldReferences.get(fieldName), e))
                .map(newElement -> wrapCatch(
                        () -> lovElementRepository.save(newElement),
                        -1
                ).getId())
                .collect(Collectors.toList());
    }

    /**
     * Find all the LOV elements by domain and field reference
     *
     * @param lovDomainDTO the domain of the LOV element
     * @param fieldName    the field name
     * @return the list of LOV elements
     */
    public List<LOVElementDTO> findAllByDomainAndFieldReference(LOVDomainTypeDTO lovDomainDTO, String fieldName) {
        var fieldReferences = getLOVFieldReference(lovDomainDTO);
        assertion(
                LOVFieldReferenceNotFound.byFieldName().errorCode(-1).fieldName(fieldName).build(),
                () -> fieldReferences.containsKey(fieldName)
        );
        return lovElementRepository.findByDomainAndFieldReference(
                        lovMapper.toLOVDomainType(lovDomainDTO),
                        fieldReferences.get(fieldName)
                )
                .stream()
                .map(lovMapper::toDTO).toList();
    }

    /**
     * Find  the LOV value by his unique id
     *
     * @param id the id of the love to retrieve
     * @return the list of LOV elements
     */
    public String findLovValueById(String id) {
        return wrapCatch(
                ()->lovElementRepository.
                        findById(id)
                        .map(LOVElement::getValue)
                        .orElseThrow(
                        ()-> LOVValueNotFound
                                .byId()
                                .errorCode(-1)
                                .id(id)
                                .build()
                ),
                -1
        );
    }

    /**
     * Find  the LOV value by his unique id
     *
     * @param id the id of the love to retrieve
     * @return the list of LOV elements
     */
    public Optional<LOVElement> findLovValueByIdNoException(String id) {
        return wrapCatch(
                ()->lovElementRepository.
                        findById(id),
                -1
        );
    }

    /**
     * Return a full list of field/lov reference for each domain
     *
     * @param lovDomainDTO the domain for which the field reference is needed
     * @return the field reference of the LOV element
     */
    public HashMap<String, String> getLOVFieldReference(LOVDomainTypeDTO lovDomainDTO) {
        return switch(lovDomainDTO) {
            case LOVDomainTypeDTO.Work -> Arrays.stream(Work.class.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(LOVField.class))
                    .collect(Collectors.toMap(
                            Field::getName,
                            field -> field.getAnnotation(LOVField.class).fieldReference(),
                            (existing, replacement) -> existing,
                            HashMap::new
                    ));
            case LOVDomainTypeDTO.Activity -> {
                var resultHash = Arrays.stream(Activity.class.getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(LOVField.class))
                        .collect(Collectors.toMap(
                                Field::getName,
                                field -> field.getAnnotation(LOVField.class).fieldReference(),
                                (existing, replacement) -> existing,
                                HashMap::new
                        ));
                resultHash.putAll(getLOVFieldReferenceFromActivityType());
                yield resultHash;
            }
        };
    }


    /**
     * Return a full list of field/lov reference for each domain
     *
     * @return the field reference of the LOV element
     */
    private HashMap<String, String> getLOVFieldReferenceFromActivityType() {
        HashMap<String, String> result = new HashMap<>();
        activityTypeRepository.findAll().forEach(
                activityType -> {
                    if (activityType.getCustomFields() != null) {
                        activityType.getCustomFields().forEach(
                                customField -> {
                                    if (customField.getIsLov() && customField.getLovFieldReference() != null) {
                                        result.put(customField.getName(), customField.getLovFieldReference());
                                    }
                                }
                        );
                    }
                }
        );
        return result;
    }

}
