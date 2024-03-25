/*
 * -----------------------------------------------------------------------------
 * Title      : LOVElementRepositoryCustom
 * ----------------------------------------------------------------------------
 * File       : LOVElementRepositoryCustom.java
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

import edu.stanford.slac.core_work_management.model.Work;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class LOVElementRepositoryImpl implements LOVElementRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public boolean addFieldReference(String id, String fieldReference) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().push("fieldReference", fieldReference);
        var res =  mongoTemplate.updateFirst(query, update, Work.class);
        return res.getModifiedCount() > 0;
    }

    @Override
    public boolean removeFieldReference(String id, String fieldReference) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().pull("fieldReference", fieldReference);
        var res =  mongoTemplate.updateFirst(query, update, Work.class);
        return res.getModifiedCount() > 0;
    }
}
