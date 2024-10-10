package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.EventTrigger;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class EventTriggerRepositoryImpl implements EventTriggerRepositoryCustom {
    MongoTemplate mongoTemplate;

    @Override
    public void updateFireTimestampByReferenceId(String referenceId, String eventType, LocalDateTime newEventFireTimestamp) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("referenceId").is(referenceId),
                Criteria.where("typeName").is(eventType)
        );
        Update update = new Update();
        update
                .set("eventFireTimestamp", newEventFireTimestamp)
                .set("fired", false);
        mongoTemplate.updateFirst(new Query(criteria), update, EventTrigger.class);
    }

    @Override
    public EventTrigger findNextToProcess(String typeName, LocalDateTime currentDate, LocalDateTime timeoutDate) {
        // Build the criteria
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("typeName").is(typeName),
                Criteria.where("eventFireTimestamp").lte(currentDate),
                Criteria.where("fired").is(false),
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
        return mongoTemplate.findAndModify(query, update, options, EventTrigger.class);
    }

    @Override
    public void completeProcessing(String eventTypeName, String id) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("id").is(id),
                Criteria.where("typeName").is(eventTypeName)
        );
        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .unset("processingId")
                .unset("processingTimestamp")
                .set("fired", true);
        mongoTemplate.updateFirst(new Query(criteria), update, EventTrigger.class);
    }
}
