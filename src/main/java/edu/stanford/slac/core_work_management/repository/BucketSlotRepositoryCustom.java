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

import java.util.List;

public interface BucketSlotRepositoryCustom {

    /**
     * Search all the work
     *
     * @param queryParameter the query parameter
     * @return the list of work
     */
    List<BucketSlot> searchAll(BucketSlotQueryParameter queryParameter);
}
