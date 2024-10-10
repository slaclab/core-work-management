package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.EventTrigger;

import java.time.LocalDateTime;

public interface EventTriggerRepositoryCustom {
    void updateFireTimestampByReferenceId(String referenceId, String eventType, LocalDateTime newEventFireTimestamp);

    /**
     * Find the next event trigger to process
     * @param typeName the event type name
     * @param currentDate the current date
     * @param timeoutDate the date when the bucket need to be reprocessed due a timeout
     * @return the event trigger to process
     */
    EventTrigger findNextToProcess(String typeName, LocalDateTime currentDate, LocalDateTime timeoutDate);

    /**
     * Set the bucket as completed as for start event
     *
     * @param id the event trigger id
     */
    void completeProcessing(String typeName, String id);
}
