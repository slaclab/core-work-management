package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class LocationRepositoryImpl implements LocationRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    @Override
    public List<Location> findByLocationFilter(LocationFilter locationFilter) {
        if(locationFilter.getText() == null || locationFilter.getText().isEmpty()) {
            return mongoTemplate.findAll(Location.class);
        }
        var query = new Query();
        query.addCriteria(TextCriteria.forDefaultLanguage().matchingAny(locationFilter.getText()));
        return mongoTemplate.find(query, Location.class);
    }
}
