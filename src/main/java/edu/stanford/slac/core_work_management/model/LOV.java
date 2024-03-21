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

import edu.stanford.slac.core_work_management.model.value.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LOV model
 * represent an element of the list of value
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class LOV {
    private String id;
    private String name;
    private String description;
    private ValueType type;
}
