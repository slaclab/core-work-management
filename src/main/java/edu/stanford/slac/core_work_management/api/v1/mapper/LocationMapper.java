package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryElementDTO;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;
import edu.stanford.slac.core_work_management.service.DomainService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class LocationMapper {
    @Autowired
    DomainService domainService;
    public abstract Location toModel(NewLocationDTO newLocationDTO);
    public abstract LocationFilter toModel(LocationFilterDTO locationFilterDTO);


    @Mapping(target = "domain", expression = "java(toDomainDTO(location.getDomainId()))")
    public abstract LocationDTO toDTO(Location location);


    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "name", expression = "java(choiceName(newLocationDTO, externalLocationDTO))")
    @Mapping(target = "description", expression = "java(choiceDescription(newLocationDTO, externalLocationDTO))")
    public abstract Location toModel(String parentId, NewLocationDTO newLocationDTO, InventoryElementDTO externalLocationDTO);

    public String choiceName(NewLocationDTO newLocationDTO, InventoryElementDTO inventoryElementDTO) {
        return inventoryElementDTO==null?newLocationDTO.name(): inventoryElementDTO.getName();
    }
    public String choiceDescription(NewLocationDTO newLocationDTO, InventoryElementDTO inventoryElementDTO) {
        return inventoryElementDTO==null?newLocationDTO.description(): inventoryElementDTO.getDescription();
    }

    public DomainDTO toDomainDTO(String domainId) {
        if(domainId == null) return null;
        return domainService.findById(domainId);
    }
}
