/*
 * -----------------------------------------------------------------------------
 * Title      : WorkAuthorizationService
 * ----------------------------------------------------------------------------
 * File       : WorkAuthorizationService.java
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

package edu.stanford.slac.core_work_management.service.authorization;

import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BucketAuthorizationService {
    private final AuthService authService;
    private final WorkService workService;
    private final ShopGroupService shopGroupService;
    /**
     * This method is used to check if the user can create a new bucket
     *
     * @param authentication the authentication object
     * @param newBucketDTO the new bucket DTO
     * @return true if the user can create a new bucket, false otherwise
     */
    public boolean canCreate(Authentication authentication, NewBucketDTO newBucketDTO) {
        return true;
    }

    /**
     * This method is used to check if the user can find a bucket by id
     *
     * @param authentication the authentication object
     * @param bucketId the bucket id
     * @return true if the user can find a bucket by id, false otherwise
     */
    public boolean canFindOById(Authentication authentication, String bucketId) {
        return true;
    }

    /**
     * This method is used to check if the user can find all buckets
     *
     * @param authentication the authentication object
     * @param bucketId the bucket id to delete
     * @return true if the user can find all buckets, false otherwise
     */
    public boolean getCanDeleteById(Authentication authentication, String bucketId) {return true;}

    /**
     * This method is used to check if the user can update a bucket by id
     *
     * @param authentication the authentication object
     * @param bucketId the bucket id
     * @return true if the user can update a bucket by id, false otherwise
     */
    public boolean getCanUpdateById(Authentication authentication, String bucketId) {return true;}

}
