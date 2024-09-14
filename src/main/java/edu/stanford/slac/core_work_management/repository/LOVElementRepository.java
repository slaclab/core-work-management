/*
 * -----------------------------------------------------------------------------
 * Title      : LOVElementRepository
 * ----------------------------------------------------------------------------
 * File       : LOVElementRepository.java
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

import edu.stanford.slac.core_work_management.model.LOVElement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LOVElementRepository extends MongoRepository<LOVElement, String>, LOVElementRepositoryCustom {
    /**
     * Check if a group name exists
     *
     * @param groupName the group name to check
     * @return true if the group name exists, false otherwise
     */
    boolean existsByGroupNameIs(String groupName);

    /**
     * Find all the LOV elements by field reference
     *
     * @param fieldReference the field reference to search
     * @return the list of LOV elements
     */
    List<LOVElement> findByFieldReferenceContains(String fieldReference);

    /**
     * Find all the LOV elements by group name
     *
     * @param groupName the group name to search
     * @return the list of LOV elements
     */
    List<LOVElement> findByGroupNameIs(String groupName);

    /**
     * Find all the LOV elements by id and field reference
     *
     * @param id             the lov id
     * @param fieldReference the field reference to search
     * @return true if the LOV element exists, false otherwise
     */
    boolean existsByIdAndFieldReferenceContains(String id, String fieldReference);

    /**
     * Check if a fieldReference si in use
     *
     * @param fieldReference the fieldReference to check
     * @return true if the fieldReference is in use, false otherwise
     */
    boolean existsByFieldReferenceContains(String fieldReference);
}
