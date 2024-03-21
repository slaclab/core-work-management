/*
 * -----------------------------------------------------------------------------
 * Title      : CustomAttributeDTO
 * ----------------------------------------------------------------------------
 * File       : CustomAttributeDTO.java
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

import edu.stanford.slac.core_work_management.model.value.AbstractValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class CustomAttribute {
    /**
     * The identifier of the custom attribute.
     * This field is used to uniquely identify the custom attribute.
     */
    String id;
    /**
     * The value of the attribute
     */
    AbstractValue value;
}
