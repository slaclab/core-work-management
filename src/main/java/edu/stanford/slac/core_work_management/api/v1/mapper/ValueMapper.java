/*
 * -----------------------------------------------------------------------------
 * Title      : ValueMapper
 * ----------------------------------------------------------------------------
 * File       : ValueMapper.java
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

package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class ValueMapper {

//    /**
//     * Converts a list of AbstractValue objects to a list of InventoryElementAttributeValue objects with string values.
//     *
//     * @param classId                       the id of the inventory class
//     * @param inventoryElementAttributeValueDTOS the list of InventoryElementAttributeValueDTO objects to convert
//     * @return the list of InventoryElementAttributeValue objects with string values
//     * @throws InventoryClassNotFound       if the inventory class with the given id is not found
//     * @throws InventoryElementAttributeNotForClass  if the attribute is not found in the inventory class
//     * @throws ControllerLogicException     if an invalid attribute type is encountered
//     */
//    public List<AbstractValue> toElementAttributeWithClass(
//            String classId,
//            List<InventoryElementAttributeValueDTO> inventoryElementAttributeValueDTOS) {
//        List<AbstractValue> abstractAttributeList = new ArrayList<>();
//        if (inventoryElementAttributeValueDTOS == null) return abstractAttributeList;
//        InventoryClass ic = wrapCatch(
//                () -> inventoryClassRepository.findById(classId),
//                -1
//        ).orElseThrow(
//                () -> InventoryClassNotFound
//                        .classNotFoundById()
//                        .id(classId)
//                        .errorCode(-2)
//                        .build()
//        );
//
//        // check for the all attribute and convert it
//        for (var attributeValue : inventoryElementAttributeValueDTOS) {
//            var attributeFound = ic.getAttributes().stream().filter(
//                    attr -> attr.getName().compareToIgnoreCase(attributeValue.name()) == 0
//            ).findFirst().orElseThrow(
//                    () -> InventoryElementAttributeNotForClass.ieaNotForClassName()
//                            .className(ic.getName())
//                            .attributeName(attributeValue.name())
//                            .errorCode(-3)
//                            .build()
//            );
//
//            Class<? extends AbstractValue> valueType = attributeFound.getType().toClassType();
//            if (valueType.isAssignableFrom(StringValue.class)) {
//                abstractAttributeList.add(
//                        StringValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(attributeValue.value())
//                                .build()
//                );
//            } else if (valueType.isAssignableFrom(BooleanValue.class)) {
//                abstractAttributeList.add(
//                        BooleanValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(Boolean.valueOf(attributeValue.value()))
//                                .build()
//                );
//            } else if (valueType.isAssignableFrom(NumberValue.class)) {
//                abstractAttributeList.add(
//                        NumberValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(Long.valueOf(attributeValue.value()))
//                                .build()
//                );
//            } else if (valueType.isAssignableFrom(DoubleValue.class)) {
//                abstractAttributeList.add(
//                        DoubleValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(Double.valueOf(attributeValue.value()))
//                                .build()
//                );
//            } else if (valueType.isAssignableFrom(DateValue.class)) {
//                abstractAttributeList.add(
//                        DateValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(LocalDate.parse(attributeValue.value(), DateTimeFormatter.ISO_LOCAL_DATE))
//                                .build()
//                );
//            } else if (valueType.isAssignableFrom(DateTimeValue.class)) {
//                abstractAttributeList.add(
//                        DateTimeValue
//                                .builder()
//                                .name(attributeValue.name())
//                                .value(LocalDateTime.parse(attributeValue.value(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
//                                .build()
//                );
//            } else {
//                throw ControllerLogicException.builder()
//                        .errorCode(-4)
//                        .errorMessage("Invalid attribute type")
//                        .errorDomain("InventoryElementMapper::toElementAttributeWithClass")
//                        .build();
//            }
//        }
//        return abstractAttributeList;
//    }
//
//    /**
//     * Converts a list of AbstractValue objects to a list of InventoryElementAttributeValue objects with string values.
//     *
//     * @param inventoryElementAttributeClass the list of AbstractValue objects to convert
//     * @return the list of InventoryElementAttributeValue objects with string values
//     * @throws ControllerLogicException if an invalid attribute type is encountered
//     */
//    public List<InventoryElementAttributeValueDTO> toElementAttributeWithString(
//            List<AbstractValue> inventoryElementAttributeClass) {
//        List<InventoryElementAttributeValueDTO> resultList = new ArrayList<>();
//        if (inventoryElementAttributeClass == null) return resultList;
//        for (AbstractValue abstractValue : inventoryElementAttributeClass) {
//            resultList.add(getInventoryElementAttributeValueDTO(abstractValue));
//        }
//        return resultList;
//    }
//
//    /**
//     * Retrieves an InventoryElementAttributeValueDTO based on the given AbstractValue.
//     *
//     * @param abstractValue the AbstractValue to convert
//     * @return the corresponding InventoryElementAttributeValueDTO
//     * @throws ControllerLogicException if an invalid attribute type is encountered
//     */
//    protected InventoryElementAttributeValueDTO getInventoryElementAttributeValueDTO(AbstractValue abstractValue) {
//        InventoryElementAttributeValueDTO newAttributeValue = null;
//        Class<? extends AbstractValue> valueType = abstractValue.getClass();
//        if (valueType.isAssignableFrom(StringValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((StringValue) abstractValue).getValue())
//                    .build();
//        } else if (valueType.isAssignableFrom(BooleanValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((BooleanValue) abstractValue).getValue().toString())
//                    .build();
//        } else if (valueType.isAssignableFrom(NumberValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((NumberValue) abstractValue).getValue().toString())
//                    .build();
//        } else if (valueType.isAssignableFrom(DoubleValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((DoubleValue) abstractValue).getValue().toString())
//                    .build();
//        } else if (valueType.isAssignableFrom(DateValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((DateValue) abstractValue).getValue().format(DateTimeFormatter.ISO_LOCAL_DATE))
//                    .build();
//        } else if (valueType.isAssignableFrom(DateTimeValue.class)) {
//            newAttributeValue = InventoryElementAttributeValueDTO
//                    .builder()
//                    .name(abstractValue.getName())
//                    .value(((DateTimeValue) abstractValue).getValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
//                    .build();
//        } else {
//            throw ControllerLogicException.builder()
//                    .errorCode(-4)
//                    .errorMessage("Invalid attribute type")
//                    .errorDomain("InventoryElementMapper::toElementAttributeWithClass")
//                    .build();
//        }
//        return newAttributeValue;
//    }
}
