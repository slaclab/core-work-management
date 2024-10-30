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
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_AUTHORIZATION_TEMPLATE;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;

@Service
@AllArgsConstructor
public class WorkAuthorizationService {
    private final AuthService authService;
    private final WorkService workService;
    private final ShopGroupService shopGroupService;

    public boolean checkCanCreate(Authentication authentication, String domainId, NewWorkDTO newWorkDTO) {
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
    public boolean checkUpdate(Authentication authentication, String domainId, String workId, UpdateWorkDTO updateWorkDTO) {
        // get stored work for check authorization on all fields
        var currentStoredWork = wrapCatch(
                () -> workService.findWorkById(domainId, workId, WorkDetailsOptionDTO.builder().build()),
                -1
        );
        // call workflow validation
        return workService.checkWorkflowForUpdate(authentication.getCredentials().toString(), currentStoredWork, updateWorkDTO);
    }

    /**
     * Check if the user can associate a work to a bucket
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @param buketId        the bucket id
     * @param move           the move flag
     * @return true if the user can associate the work to the bucket, false otherwise
     */
    public boolean canAssociateToBucket(Authentication authentication, String domainId, String workId, String buketId, Optional<Boolean> move) {
        // get stored work for check authorization on all fields
        var currentStoredWork = wrapCatch(
                () -> workService.findWorkById(domainId, workId, WorkDetailsOptionDTO.builder().build()),
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
                                domainId,
                                workService.getShopGroupIdByWorkId(workId),
                                authentication.getCredentials().toString()
                        )
                )
        );
        // call workflow validation
        return true;
    }

    /**
     * Check if the user can create log on work
     *
     * @param authentication the authentication object
     * @param workId         the work id
     * @return true if the user can create a new work, false otherwise
     */
    public boolean checkLoggingOnWork(Authentication authentication, String domainId, String workId) {
        // get stored work for check authorization on all fields
        var currentStoredWork = wrapCatch(
                () -> workService.findWorkById(domainId, workId, WorkDetailsOptionDTO.builder().build()),
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
                                domainId,
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
    public boolean applyCompletionDTOList(ApiResultResponse<List<WorkSummaryDTO>> workDTOS, Authentication authentication) {
        List<WorkSummaryDTO> filledDTOs = workDTOS.getPayload().stream().map(
                workDTO -> {
                    // call workflow validation
                    return workDTO.toBuilder().accessList(
                            workService.getUserAuthorizationOnWork(
                                    authentication.getPrincipal().toString(),
                                    workDTO
                            )
                    ).build();
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
        var w = workDTO.getPayload();
        workDTO.setPayload(
                workDTO.getPayload()
                        .toBuilder()
                        .accessList
                                (
                                        workService.getUserAuthorizationOnWork
                                                (
                                                        authentication.getPrincipal().toString(),
                                                        w
                                                )
                                )
                        .build()
        );
        return true;
    }

}
