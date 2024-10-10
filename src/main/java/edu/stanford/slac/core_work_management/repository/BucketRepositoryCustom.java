/*
 * -----------------------------------------------------------------------------
 * Title      : WorkRepositoryCustom
 * ----------------------------------------------------------------------------
 * File       : WorkRepositoryCustom.java
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

import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotQueryParameter;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BucketRepositoryCustom {

    /**
     * Search all the work
     *
     * @param queryParameter the query parameter
     * @return the list of work
     */
    List<BucketSlot> searchAll(BucketSlotQueryParameter queryParameter);

    /**
     * Find all the bucket slot that contains the date
     *
     * @param from the date to search
     * @return the list of bucket slot
     */
    List<BucketSlot> findAllThatContainsDate(LocalDateTime from);

    /**
     * Find the next bucket that need to manage to be started up
     *
     * @param currentDate the current date
     * @param timeoutDate the date when the bucket need to be reprocessed due a timeout
     * @return the bucket to startup
     */
    BucketSlot findNextBucketToStart(LocalDateTime currentDate, LocalDateTime timeoutDate);

    /**
     * Find the next bucket that need to manage to be stopped
     *
     * @param currentDate the current date
     * @param timeoutDate the date when the bucket need to be reprocessed due a timeout
     * @return the bucket to stop
     */
    BucketSlot findNextBucketToStop(LocalDateTime currentDate, LocalDateTime timeoutDate);

    /**
     * Set the bucket as completed as for start event
     *
     * @param bucketSlotId the bucket slot id
     */
    void completeStartEventProcessing(String bucketSlotId);

    /**
     * Set the bucket as completed as for stop event
     *
     * @param bucketSlotId the bucket slot id
     */
    void completeStopEventProcessing(String bucketSlotId);
}
