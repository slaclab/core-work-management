/*
 * -----------------------------------------------------------------------------
 * Title      : LOVMapper
 * ----------------------------------------------------------------------------
 * File       : LOVMapper.java
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

import edu.stanford.slac.core_work_management.api.v1.dto.BucketSlotDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVValueDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketSlotDTO;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.service.LOVService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class BucketSlotMapper {
    @Autowired
    private LOVService lovService;

    public abstract BucketSlot toModel(NewBucketSlotDTO dto);

    @Mapping(target = "bucketType",  qualifiedByName = "toLOVValueDTO")
    @Mapping(target = "bucketStatus", qualifiedByName = "toLOVValueDTO")
    public abstract BucketSlotDTO toDTO(BucketSlot bucketSlot);

    /**
     * Convert static string field to {@link }LOVValueDTO}
     *
     * @param value the id of the lov value
     * @return the value from lov if found
     */
    @Named("toLOVValueDTO")
    public LOVValueDTO toLOVValueDTO(String value) {
        if (value == null) return null;
        var valueFound = lovService.findLovValueById(value);
        return LOVValueDTO
                .builder()
                .id(
                        value
                )
                .value(
                        valueFound
                )
                .build();
    }
}
