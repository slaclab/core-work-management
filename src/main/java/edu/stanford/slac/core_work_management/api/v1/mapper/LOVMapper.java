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

import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLOVElementDTO;
import edu.stanford.slac.core_work_management.model.LOVDomainType;
import edu.stanford.slac.core_work_management.model.LOVElement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class LOVMapper {
    public abstract LOVDomainType toLOVDomainType(LOVDomainTypeDTO domainTypeDTO);
    @Mapping(target = "value", source = "lovElementDTO.value")
    @Mapping(target = "description", source = "lovElementDTO.description")
    public abstract LOVElement toModel(LOVDomainTypeDTO domain, List<String> fieldReference, NewLOVElementDTO lovElementDTO);
    public abstract LOVElementDTO toDTO(LOVElement lovElement);

    public abstract LOVElement toModelByGroupName(String groupName, NewLOVElementDTO e);
}
