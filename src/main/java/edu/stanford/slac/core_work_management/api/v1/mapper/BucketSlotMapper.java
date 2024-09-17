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

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotQueryParameter;
import edu.stanford.slac.core_work_management.model.BucketSlotWorkType;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class BucketSlotMapper {
    @Autowired
    private LOVService lovService;
    @Autowired
    private DomainService domainService;

    /**
     * Convert a new bucket slot DTO to a bucket slot model
     *
     * @param dto the new bucket slot DTO
     * @return the bucket slot model
     */
    public abstract BucketSlot toModel(NewBucketDTO dto);

    /**
     * Convert a bucket slot query parameter DTO to a bucket slot query parameter model
     *
     * @param dto the bucket slot query parameter DTO
     * @return the bucket slot query parameter model
     */
    public abstract BucketSlotQueryParameter toModel(BucketQueryParameterDTO dto);

    /**
     * Convert a bucket slot model to a bucket slot DTO
     *
     * @param bucketSlot the bucket slot model
     * @return the bucket slot DTO
     */
    @Mapping(target = "type", qualifiedByName = "toLOVValueDTO")
    @Mapping(target = "status", qualifiedByName = "toLOVValueDTO")
    @Mapping(target = "admittedWorkType", expression = "java(getWorkType(bucketSlot.getAdmittedWorkTypeIds()))")
    public abstract BucketSlotDTO toDTO(BucketSlot bucketSlot);

    /**
     * Create a list of bucket slot DTO from a list of bucket slot ids
     *
     * @param workTypeIds the set of work type ids
     * @return the set of work type DTO
     */
    public Set<WorkTypeDTO> getWorkType(Set<BucketSlotWorkType> workTypeIds) {
        Set<WorkTypeDTO> result = new HashSet<>();
        if (workTypeIds == null) return result;
        workTypeIds.forEach(
                wt -> result.add(
                            domainService.findWorkTypeById(wt.getDomainId(), wt.getWorkTypeId())
                    )
        );
        return result;
}

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
