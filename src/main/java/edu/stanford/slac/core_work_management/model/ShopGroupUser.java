/*
 * -----------------------------------------------------------------------------
 * Title      : ShopGroupUserDTO
 * ----------------------------------------------------------------------------
 * File       : ShopGroupUserDTO.java
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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.model.Person;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

/**
 * ShopGroupUserDTO, a group of people that is in charge of fixing a problem
 */
@Data
@Builder(toBuilder = true)
public class ShopGroupUser {
    /**
     * The user that is part of the shop group
     */
    private PersonDTO user;
    /**
     * The role of the user in the shop group
     */
    private Boolean isLeader;
    @CreatedDate
    private LocalDateTime createdDate;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    @LastModifiedBy
    private String lastModifiedBy;
    @Version
    private Long version;
}
