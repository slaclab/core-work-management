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
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotQueryParameter;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Repository
@AllArgsConstructor
public class BucketRepositoryImpl implements BucketRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    /**
     * Search all the work
     *
     * @param queryParameter the query parameter
     * @return the list of work
     */
    @Override
    public List<BucketSlot> searchAll(BucketSlotQueryParameter queryParameter) {
        if (
                queryParameter.getContextSize() != null &&
                        queryParameter.getContextSize() > 0 &&
                        queryParameter.getAnchorID() == null
        ) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The context count cannot be used without the anchor")
                    .errorDomain("WorkRepositoryImpl::searchAll")
                    .build();
        }

        // all the criteria
        List<Criteria> allCriteria = new ArrayList<>();
        LocalDateTime anchorCreatedDate = queryParameter.getAnchorID() != null ? getAnchorCreatedDate(queryParameter.getAnchorID()) : null;
        List<BucketSlot> elementsBeforeAnchor = contextSearch(queryParameter, anchorCreatedDate, allCriteria);
        List<BucketSlot> elementsAfterAnchor = limitSearch(queryParameter, anchorCreatedDate, allCriteria);
        elementsBeforeAnchor.addAll(elementsAfterAnchor);
        return elementsBeforeAnchor;
    }

    /**
     * Find all the bucket that contains the date
     */
    @Override
    public List<BucketSlot> findAllThatContainsDate(LocalDateTime date) {
        Query query = new Query();
        query.addCriteria(Criteria.where("from").lte(date).and("to").gte(date));
        return mongoTemplate.find(query, BucketSlot.class);
    }

    @Override
    public BucketSlot findNextBucketToStart(LocalDateTime currentDate, LocalDateTime timeoutDate) {
        // Build the criteria
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("from").lte(currentDate),
                Criteria.where("startEventManaged").is(false),
                new Criteria().orOperator(
                        Criteria.where("processingId").is(null),
                        Criteria.where("processingTimestamp").lt(timeoutDate)
                )
        );

        Query query = new Query(criteria);
        query.limit(1); // Limit to one document

        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .set("processingId", UUID.randomUUID().toString())
                .set("processingTimestamp", timeoutDate);

        // Options to return the new document after update
        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(true)
                .upsert(false);

        return mongoTemplate.findAndModify(query, update, options, BucketSlot.class);
    }

    @Override
    public BucketSlot findNextBucketToStop(LocalDateTime currentDate, LocalDateTime timeoutDate) {
        // Build the criteria
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("to").lte(currentDate),
                Criteria.where("stopEventManaged").is(false),
                new Criteria().orOperator(
                        Criteria.where("processingId").is(null),
                        Criteria.where("processingTimestamp").lt(timeoutDate)
                )
        );

        Query query = new Query(criteria);
        query.limit(1); // Limit to one document

        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .set("processingId", UUID.randomUUID().toString())
                .set("processingTimestamp", timeoutDate);

        // Options to return the new document after update
        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(true)
                .upsert(false);

        return mongoTemplate.findAndModify(query, update, options, BucketSlot.class);
    }

    @Override
    public void completeStartEventProcessing(String bucketSlotId) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("id").is(bucketSlotId)
        );
        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .unset("processingId")
                .unset("processingTimestamp")
                .set("startEventManaged", true);
        mongoTemplate.updateFirst(new Query(criteria), update, BucketSlot.class);
    }

    @Override
    public void completeStopEventProcessing(String bucketSlotId) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("id").is(bucketSlotId)
        );
        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .unset("processingId")
                .unset("processingTimestamp")
                .set("stopEventManaged", true);
        mongoTemplate.updateFirst(new Query(criteria), update, BucketSlot.class);
    }

    /**
     * Get the query to search the work
     *
     * @param anchorId the query parameter
     * @return the query
     */
    private LocalDateTime getAnchorCreatedDate(String anchorId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("id").is(anchorId));
        q.fields().include("createdDate");
        var slotFound = mongoTemplate.findOne(q, BucketSlot.class);
        return (slotFound != null) ? slotFound.getCreatedDate() : null;
    }

    /**
     * Get the default query
     *
     * @param queryParameter is the query parameter class
     * @return return the mongodb query
     */
    private static Query getQuery(BucketSlotQueryParameter queryParameter) {
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
     *
     * @param queryParameter    the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria       the criteria
     * @return the list of work
     */
    private List<BucketSlot> limitSearch(BucketSlotQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<BucketSlot> elementsAfterAnchor = new ArrayList<>();
        if (queryParameter.getLimit() != null && queryParameter.getLimit() > 0) {
            Query query = getQuery(queryParameter);
            if (anchorCreatedDate != null) {
                allCriteria.add(
                        Criteria.where("createdDate").lt(anchorCreatedDate)
                );
            }
            if (queryParameter.getFrom() != null) {
                allCriteria.add(
                        Criteria.where("from").gte(queryParameter.getFrom())
                );
            }
            if (!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }

            query
                    .with(
                            Sort.by(
                                    Sort.Direction.ASC, "from")
                    )
                    .limit(queryParameter.getLimit());
            elementsAfterAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            BucketSlot.class
                    )
            );
        }
        return elementsAfterAnchor;
    }

    /**
     * Search the context
     *
     * @param queryParameter    the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria       the criteria
     * @return the list of work
     */
    private List<BucketSlot> contextSearch(BucketSlotQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<BucketSlot> elementsBeforeAnchor = new ArrayList<>();
        if (
                queryParameter.getContextSize() != null
                        && queryParameter.getContextSize() > 0
                        && anchorCreatedDate != null
        ) {
            allCriteria.add(
                    Criteria.where("createdDate").gte(anchorCreatedDate)
            );
            if (queryParameter.getFrom() != null) {
                allCriteria.add(
                        Criteria.where("from").lte(queryParameter.getFrom())
                );
            }
            // at this point the anchor id is not null
            Query query = getQuery(queryParameter);
            if (!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }
            query.with(
                    Sort.by(
                            Sort.Direction.ASC, "from")
            ).limit(queryParameter.getContextSize());
            elementsBeforeAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            BucketSlot.class
                    )
            );
            // reverse the order
            Collections.reverse(elementsBeforeAnchor);
        }
        return elementsBeforeAnchor;
    }
}
