package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.service.StringUtility;
import org.mapstruct.*;

import javax.swing.text.Utilities;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class DomainMapper {
    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    public abstract Domain toModel(NewDomainDTO newDomainDTO);

    public abstract DomainDTO toDTO(Domain domain);

    @Named("normalizeName")
    public String modifyName(String name) {
        return name.trim().toLowerCase().replace(" ", "-");
    }
}