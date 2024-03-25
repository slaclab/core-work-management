/*
 * -----------------------------------------------------------------------------
 * Title      : LOV
 * ----------------------------------------------------------------------------
 * File       : LOV.java
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

package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * LOV model
 * represent an element of the list of value
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class LOVElement {
    /**
     * The id of the LOV element
     */
    private String id;
    /**
     * The domain of the LOV element
     * identify the model where the LOV element is used
     */
    private LOVDomainType domain;
    /**
     *
     */
    private String groupName;
    /**
     * The field reference of the LOV element
     * identify the field where the LOV element is used
     */
    @Builder.Default
    private List<String> fieldReference = new ArrayList<>();
    /**
     * The value of the LOV element
     */
    private String value;
    /**
     * The description of the LOV element
     */
    private String description;
}
