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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_AUTHORIZATION_TEMPLATE;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;
import static java.util.Collections.emptyList;

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
        // get stored work for check authorization on all fields
        var currentStoredWork = wrapCatch(
                () -> workService.findWorkById(workId, WorkDetailsOptionDTO.builder().build()),
                -1
        );
        boolean isRoot = authService.checkForRoot(authentication);
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
                        () -> isRoot,
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

        // only admin can update the location
        if (updateWorkDTO.locationId() != null) {
            assertion(
                    NotAuthorized.notAuthorizedBuilder()
                            .errorCode(-1)
                            .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, updateWorkDTO)")
                            .build(),
                    // should be one of these
                    () -> any(
                            // a root users
                            () -> isRoot,
                            // or a user that has the right as admin on the work
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    AuthorizationTypeDTO.Admin,
                                    WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                            )
                    )
            );
        }
        // only group leader can update the assigned to
        if (updateWorkDTO.assignedTo() != null && !updateWorkDTO.assignedTo().isEmpty()) {
            assertion(
                    NotAuthorized.notAuthorizedBuilder()
                            .errorCode(-1)
                            .errorDomain("WorkAuthorizationService::checkUpdate(authentication, workId, updateWorkDTO)")
                            .build(),
                    // should be one of these
                    () -> any(
                            // a root users
                            () -> isRoot,
                            // or a user that is the leader of the group
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    AuthorizationTypeDTO.Admin,
                                    SHOP_GROUP_AUTHORIZATION_TEMPLATE.formatted(currentStoredWork.shopGroup().id())
                            )
                    )
            );
        }
        return true;
    }

    /**
     * Check if the user can create log on work
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @return true if the user can create a new work, false otherwise
     */
    public boolean checkLoggingOnWork(Authentication authentication, String workId) {
        // get stored work for check authorization on all fields
        var currentStoredWork = wrapCatch(
                () -> workService.findWorkById(workId, WorkDetailsOptionDTO.builder().build()),
                -1
        );
        boolean isRoot = authService.checkForRoot(authentication);
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("WorkAuthorizationService::checkLogging(authentication, workId)")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be one of these
                () -> any(
                        // a root users
                        () -> isRoot,
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
        return true;
    }

    /**
     * Check if the user can close a work
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param reviewWorkDTO  the close work dto
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
     * Check if the user can create log on activity
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param activityId     the activity id
     * @return true if the user can create a new work, false otherwise
     */
    public boolean checkLoggingOnActivity(Authentication authentication, String workId, String activityId) {
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
     * @param authentication the authentication object
     * @return true if the user can update the status of the activity, false otherwise
     */
    public boolean applyCompletionDTOList(ApiResultResponse<List<WorkDTO>> workDTOS, Authentication authentication) {
        List<WorkDTO> filledDTOs = workDTOS.getPayload().stream().map(
                workDTO -> {
                    // check for auth
                    var authList = workService.getAuthorizationByWork(workDTO, authentication);
                    return workDTO.toBuilder().accessList(authList).build();
                }
        ).toList();
        workDTOS.setPayload(filledDTOs);
        return true;
    }

    /**
     * Check if the user can update the status of an activity
     *
     * @param authentication the authentication object
     * @return true if the user can update the status of the activity, false otherwise
     */
    public boolean applyCompletionDTO(ApiResultResponse<WorkDTO> workDTO, Authentication authentication) {
        workDTO.setPayload(
                workDTO.getPayload()
                        .toBuilder()
                        .accessList
                                (
                                        workService.getAuthorizationByWork
                                                (
                                                        workDTO.getPayload(),
                                                        authentication
                                                )
                                )
                        .build()
        );
        return true;
    }
}
