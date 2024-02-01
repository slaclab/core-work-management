package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryElementDTO;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class LocationMapper {
    public abstract Location toModel(NewLocationDTO newLocationDTO);
    public abstract LocationFilter toModel(LocationFilterDTO locationFilterDTO);
    public abstract LocationDTO toDTO(Location location);

    @Mapping(target = "parentId", source = "newLocationDTO.parentId")
    @Mapping(target = "name", expression = "java(choiceName(newLocationDTO, externalLocationDTO))")
    @Mapping(target = "description", expression = "java(choiceDescription(newLocationDTO, externalLocationDTO))")
    public abstract Location toModel(NewLocationDTO newLocationDTO, InventoryElementDTO externalLocationDTO);

    public String choiceName(NewLocationDTO newLocationDTO, InventoryElementDTO inventoryElementDTO) {
        return inventoryElementDTO==null?newLocationDTO.name(): inventoryElementDTO.getName();
    }
    public String choiceDescription(NewLocationDTO newLocationDTO, InventoryElementDTO inventoryElementDTO) {
        return inventoryElementDTO==null?newLocationDTO.description(): inventoryElementDTO.getDescription();
    }
}
