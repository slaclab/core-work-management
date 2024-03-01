/*
 * -----------------------------------------------------------------------------
 * Title      : BaseAuthorizationService
 * ----------------------------------------------------------------------------
 * File       : BaseAuthorizationService.java
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

/*
 * -----------------------------------------------------------------------------
 * Title      : BaseAuthorizationService
 * ----------------------------------------------------------------------------
 * File       : BaseAuthorizationService.java
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

/*
 * -----------------------------------------------------------------------------
 * Title      : BaseAuthorizationService
 * ----------------------------------------------------------------------------
 * File       : BaseAuthorizationService.java
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

import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class BaseAuthorizationService {
    private final AuthService authService;
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
                        .errorDomain("BaseAuthorizationService::checkAuthenticated")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication)
        );
        return true;
    }

    /**
     * Check if the user is authenticated
     *
     * @param authentication the authentication object
     * @return true if the user is authenticated, false otherwise
     */
    public boolean checkForRoot(Authentication authentication) {
        // check for auth
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("BaseAuthorizationService::checkForRoot")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication)
        );
        return true;
    }
}
