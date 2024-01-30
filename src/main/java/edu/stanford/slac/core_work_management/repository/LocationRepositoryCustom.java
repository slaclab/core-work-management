package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;

import java.util.List;

public interface LocationRepositoryCustom {
    List<Location> findByLocationFilter(LocationFilter locationFilter);
}
