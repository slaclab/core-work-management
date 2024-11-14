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
import edu.stanford.slac.core_work_management.model.Counter;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkQueryParameter;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
@AllArgsConstructor
public class WorkRepositoryImpl implements WorkRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    /**
     * Search all the work
     * @param queryParameter the query parameter
     * @return the list of work
     */
    @Override
    public List<Work> searchAll(WorkQueryParameter queryParameter) {
        if (
                queryParameter.getContextSize() != null &&
                        queryParameter.getContextSize() >0 &&
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
        LocalDateTime anchorCreatedDate = queryParameter.getAnchorID() != null?getAnchorCreatedDate(queryParameter.getAnchorID()):null;
        List<Work> elementsBeforeAnchor = contextSearch(queryParameter, anchorCreatedDate, allCriteria);
        List<Work> elementsAfterAnchor =  limitSearch(queryParameter, anchorCreatedDate, allCriteria);
        elementsBeforeAnchor.addAll(elementsAfterAnchor);
        return elementsBeforeAnchor;
    }

    @Override
    @Retryable(
            value = {MongoTransactionException.class, UncategorizedMongoDbException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Long getNextWorkId() {
        // Define the query to find the counter document based on its id (sequenceName)
        Query query = new Query(Criteria.where("id").is(Work.class.getSimpleName()));
        // Define the update operation to increment the counter
        Update update = new Update().inc("sequence", 1);
        // Ensure the option to return the new incremented value and to upsert
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        // Execute the findAndModify operation, which will atomically increment the counter and create it if it doesn't exist
        Counter counter = mongoTemplate.findAndModify(query, update, options, Counter.class);
        // Return the next sequence number
        return Objects.requireNonNull(counter).getSequence();
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
        var inventoryElementFound =  mongoTemplate.findOne(q, Work.class);
        return (inventoryElementFound!=null)?inventoryElementFound.getCreatedDate():null;
    }

    /**
     * Get the default query
     * @param queryParameter is the query parameter class
     * @return return the mongodb query
     */
    private static Query getQuery(WorkQueryParameter queryParameter) {
        Query query;
        if (queryParameter.getSearch() != null && !queryParameter.getSearch().isEmpty()) {
            query = TextQuery.queryText(TextCriteria.forDefaultLanguage()
                    .matchingAny(queryParameter.getSearch().split(" "))
            );
        } else {
            query = new Query();
        }
        if(queryParameter.getDomainIds() != null && !queryParameter.getDomainIds().isEmpty()) {
            query.addCriteria(
                    Criteria.where("domainId").in(queryParameter.getDomainIds())
            );
        }

        if (queryParameter.getWorkTypeIds() != null && !queryParameter.getWorkTypeIds().isEmpty()) {
            query.addCriteria(
                    Criteria.where("workType.id").in(queryParameter.getWorkTypeIds())
            );
        }

        if (queryParameter.getCreatedBy() != null && !queryParameter.getCreatedBy().isEmpty()) {
            query.addCriteria(
                    Criteria.where("createdBy").in(queryParameter.getCreatedBy())
            );
        }

        if (queryParameter.getAssignedTo() != null && !queryParameter.getAssignedTo().isEmpty()) {
            query.addCriteria(
                    Criteria.where("assignedTo").in(queryParameter.getAssignedTo())
            );
        }

        if (queryParameter.getWorkflowName() != null && !queryParameter.getWorkflowName().isEmpty()) {
            query.addCriteria(
                    Criteria.where("workType.workflow.name").in(queryParameter.getWorkflowName())
            );
        }

        if (queryParameter.getWorkflowState() != null && !queryParameter.getWorkflowState().isEmpty()) {
            query.addCriteria(
                    Criteria.where("currentStatus.status").in(queryParameter.getWorkflowState())
            );
        }

        if (queryParameter.getBucketId() != null && !queryParameter.getBucketId().isEmpty()) {
            query.addCriteria(
                    Criteria.where("currentBucketAssociation.bucketId").in(queryParameter.getBucketId())
            );
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
    private List<Work> limitSearch(WorkQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<Work> elementsAfterAnchor = new ArrayList<>();
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
                            Work.class
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
    private List<Work> contextSearch(WorkQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<Work> elementsBeforeAnchor = new ArrayList<>();
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
                            Work.class
                    )
            );
            // reverse the order
            Collections.reverse(elementsBeforeAnchor);
        }
        return elementsBeforeAnchor;
    }
}
