package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class LocationMapper {
    public abstract Location toModel(NewLocationDTO newLocationDTO);
    public abstract LocationFilter toModel(LocationFilterDTO locationFilterDTO);
    public abstract LocationDTO toDTO(Location location);
}
