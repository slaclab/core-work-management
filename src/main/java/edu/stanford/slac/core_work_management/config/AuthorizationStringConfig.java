/*
 * -----------------------------------------------------------------------------
 * Title      : AuthorizationStringConfig
 * ----------------------------------------------------------------------------
 * File       : AuthorizationStringConfig.java
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

package edu.stanford.slac.core_work_management.config;

/**
 * This class contains the authorization string templates
 * to be used in the application

 */
public class AuthorizationStringConfig {
    /**
     * The work authorization template
     */
    public static final String WORK_AUTHORIZATION_TEMPLATE = "/cwm/work/%s";

    /**
     * The shop group authorization template
     */
    public static final String SHOP_GROUP_AUTHORIZATION_TEMPLATE = "/cwm/shop-group/%s";

    /**
     * The fake user for the shop group id to be used in the authorization check
     * in this way we can use the shop group id as an user
     */
    public static final String SHOP_GROUP_FAKE_USER_TEMPLATE = "%s@shopgroup.cws.slac.stanford.edu";
}
