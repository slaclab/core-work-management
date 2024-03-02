/*
 * -----------------------------------------------------------------------------
 * Title      : WorkRepositoryImpl
 * ----------------------------------------------------------------------------
 * File       : WorkRepositoryImpl.java
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

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityQueryParameter;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkQueryParameter;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@AllArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    /**
     * Search all the work
     * @param queryParameter the query parameter
     * @return the list of work
     */
    @Override
    public List<Activity> searchAll(ActivityQueryParameter queryParameter) {
        if (
                queryParameter.getContextSize() != null &&
                        queryParameter.getContextSize() >0 &&
                        queryParameter.getAnchorID() == null
        ) {
            throw ControllerLogicException.of(
                    -1,
                    "The context count cannot be used without the anchor",
                    "WorkRepositoryImpl::searchAll"
            );
        }

        // all the criteria
        List<Criteria> allCriteria = new ArrayList<>();
        LocalDateTime anchorCreatedDate = queryParameter.getAnchorID() != null?getAnchorCreatedDate(queryParameter.getAnchorID()):null;
        List<Activity> elementsBeforeAnchor = contextSearch(queryParameter, anchorCreatedDate, allCriteria);
        List<Activity> elementsAfterAnchor =  limitSearch(queryParameter, anchorCreatedDate, allCriteria);
        elementsBeforeAnchor.addAll(elementsAfterAnchor);
        return elementsBeforeAnchor;
    }

    /**
     * Get the query to search the work
     * @param anchorId the query parameter
     * @return the query
     */
    private LocalDateTime getAnchorCreatedDate(String anchorId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("id").is(anchorId));
        q.fields().include("createdDate");
        var inventoryElementFound =  mongoTemplate.findOne(q, Activity.class);
        return (inventoryElementFound!=null)?inventoryElementFound.getCreatedDate():null;
    }

    /**
     * Get the default query
     * @param queryParameter is the query parameter class
     * @return return the mongodb query
     */
    private static Query getQuery(ActivityQueryParameter queryParameter) {
        Query query;
        if (queryParameter.getSearch() != null && !queryParameter.getSearch().isEmpty()) {
            query = TextQuery.queryText(TextCriteria.forDefaultLanguage()
                    .matchingAny(queryParameter.getSearch().split(" "))
            );
        } else {
            query = new Query();
        }
        return query;
    }

    /**
     * Limit the search
     * @param queryParameter the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria the criteria
     * @return the list of work
     */
    private List<Activity> limitSearch(ActivityQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<Activity> elementsAfterAnchor = new ArrayList<>();
        if (queryParameter.getLimit() != null && queryParameter.getLimit() > 0) {
            Query query = getQuery(queryParameter);
            if (anchorCreatedDate != null) {
                allCriteria.add(
                        Criteria.where("createdDate").gt(anchorCreatedDate)
                );
            }
            if(!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }

            query.with(
                    Sort.by(
                            Sort.Direction.ASC, "createdDate")
            ).limit(queryParameter.getLimit());
            elementsAfterAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            Activity.class
                    )
            );
        }
        return elementsAfterAnchor;
    }

    /**
     * Search the context
     * @param queryParameter the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria the criteria
     * @return the list of work
     */
    private List<Activity> contextSearch(ActivityQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<Activity> elementsBeforeAnchor = new ArrayList<>();
        if (
                queryParameter.getContextSize() != null
                        && queryParameter.getContextSize() > 0
                        && anchorCreatedDate != null
        ) {
            allCriteria.add(
                    Criteria.where("createdDate").lte(anchorCreatedDate)
            );

            // at this point the anchor id is not null
            Query query = getQuery(queryParameter);
            if(!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }
            query.with(
                    Sort.by(
                            Sort.Direction.DESC, "createdDate")
            ).limit(queryParameter.getContextSize());
            elementsBeforeAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            Activity.class
                    )
            );
            // reverse the order
            Collections.reverse(elementsBeforeAnchor);
        }
        return elementsBeforeAnchor;
    }
}
