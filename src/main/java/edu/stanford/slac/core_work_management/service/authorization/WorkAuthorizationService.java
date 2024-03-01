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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.ReviewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityStatusDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;

@Service
@AllArgsConstructor
public class WorkAuthorizationService {
    private final AuthService authService;
    private final WorkService workService;
    private final ShopGroupService shopGroupService;

    /**
     * Check if the user is authenticated
     *
     * @param authentication the authentication object
     * @return true if the user is authenticated, false otherwise
     */
    public boolean checkAuthenticated(Authentication authentication) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkAuthenticated")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication)
        );
        return true;
    }

    /**
     * Check if the user can create a new work
     *
     * @param authentication the authentication object
     * @return true if the user can create a new work, false otherwise
     */
    public boolean checkCreateNewActivity(Authentication authentication, String workId) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkCreateNewActivity")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> authService.checkForRoot(authentication),
                        // or a user that has the right to write on the work
                        () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(authentication, AuthorizationTypeDTO.Write, WORK_AUTHORIZATION_TEMPLATE.formatted(workId)),
                        // user of the shop group are always treated as admin on the work
                        () -> shopGroupService.checkContainsAUserEmail(
                                // fire not found work exception
                                workService.getShopGroupIdByWorkId(workId),
                                authentication.getCredentials().toString()
                        )
                )
        );
        return true;
    }

    /**
     * Check if the user can update a work
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param updateWorkDTO  the update work dto
     * @return true if the user can update the work, false otherwise
     */
    public boolean checkUpdate(Authentication authentication, String workId, UpdateWorkDTO updateWorkDTO) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, updateWorkDTO)")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> authService.checkForRoot(authentication),
                        // or a user that has the right as writer on the work
                        () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                AuthorizationTypeDTO.Write,
                                WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                        ),
                        // user of the shop group are always treated as admin on the work
                        () -> shopGroupService.checkContainsAUserEmail(
                                // fire not found work exception
                                workService.getShopGroupIdByWorkId(workId),
                                authentication.getCredentials().toString()
                        )
                )
        );

        // check for only admin update
        if(updateWorkDTO.locationId() != null) {
            assertion(
                    NotAuthorized.notAuthorizedBuilder()
                            .errorCode(-1)
                            .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, updateWorkDTO)")
                            .build(),
                    // should be one of these
                    () -> any(
                            // a root users
                            () -> authService.checkForRoot(authentication),
                            // or a user that has the right as admin on the work
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    AuthorizationTypeDTO.Admin,
                                    WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                            )
                    )
            );
        }
        return true;
    }

    /**
     * Check if the user can close a work
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param reviewWorkDTO   the close work dto
     * @return true if the user can close the work, false otherwise
     */
    public boolean checkReviewWork(Authentication authentication, String workId, ReviewWorkDTO reviewWorkDTO) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkCloseWork(authentication, workId, closeWorkDTO)")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> authService.checkForRoot(authentication),
                        // or a user that has the right as admin on the work
                        () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                AuthorizationTypeDTO.Admin,
                                WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                        )
                )
        );
        return true;
    }

    /**
     * Check if the user can update an activity
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param activityId     the activity id
     * @param updateWorkDTO  the update work dto
     * @return true if the user can update the activity, false otherwise
     */
    public boolean checkUpdate(Authentication authentication, String workId, String activityId, UpdateActivityDTO updateWorkDTO) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, activityId, updateWorkDTO)")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> authService.checkForRoot(authentication),
                        // or a user that has the right to write on the work
                        () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                AuthorizationTypeDTO.Write,
                                WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                        ),
                        // user of the shop group are always treated as admin on the work
                        () -> shopGroupService.checkContainsAUserEmail(
                                // fire not found work exception
                                workService.getShopGroupIdByWorkId(workId),
                                authentication.getCredentials().toString()
                        )
                )
        );
        return true;
    }

    /**
     * Check if the user can update the status of an activity
     *
     * @param authentication            the authentication object
     * @param workId                    the work id
     * @param activityId                the activity id
     * @param updateActivityStatusDTO   the update activity status dto
     * @return true if the user can update the status of the activity, false otherwise
     */
    public boolean checkUpdateStatus(Authentication authentication, String workId, String activityId, UpdateActivityStatusDTO updateActivityStatusDTO) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, activityId, updateWorkDTO)")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> authService.checkForRoot(authentication),
                        // or a user that has the right to write on the work
                        () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                AuthorizationTypeDTO.Write,
                                WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                        ),
                        // user of the shop group are always treated as admin on the work
                        () -> shopGroupService.checkContainsAUserEmail(
                                // fire not found work exception
                                workService.getShopGroupIdByWorkId(workId),
                                authentication.getCredentials().toString()
                        )
                )
        );
        return true;
    }
}
