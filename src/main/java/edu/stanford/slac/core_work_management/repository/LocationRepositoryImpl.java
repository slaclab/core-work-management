
/*
 * -----------------------------------------------------------------------------
 * Title      : LocationRepositoryImpl
 * ----------------------------------------------------------------------------
 * File       : LocationRepositoryImpl.java
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

package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.LocationFilter;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * This class is used to implement the custom query for the LocationRepository
 */
@Repository
@AllArgsConstructor
public class LocationRepositoryImpl implements LocationRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    /**
     * This method is used to find the location by the location filter
     * @param locationFilter the location filter
     * @return List<Location>
     */
    @Override
    public List<Location> findByLocationFilter(LocationFilter locationFilter) {
        var query = new Query();
        if(locationFilter.getText() != null && !locationFilter.getText().isEmpty()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matchingAny(locationFilter.getText()));
        }

        if(locationFilter.getExternalId() != null && !locationFilter.getExternalId().isEmpty()) {
            query.addCriteria(
                    Criteria.where("externalLocationIdentifier").is(locationFilter.getExternalId())
            );
        }
        return mongoTemplate.find(query, Location.class);
    }
}
