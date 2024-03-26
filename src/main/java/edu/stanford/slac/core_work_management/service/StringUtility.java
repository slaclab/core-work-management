/*
 * -----------------------------------------------------------------------------
 * Title      : Utility
 * ----------------------------------------------------------------------------
 * File       : Utility.java
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

package edu.stanford.slac.core_work_management.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtility {
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Step 1: Lowercase the entire string to handle mixed case inputs uniformly
        String temp = input.toLowerCase();

        // Step 2: Use regex to find sequences that should lead to an uppercase letter
        Matcher matcher = Pattern.compile("([^a-zA-Z])([a-z])").matcher(temp);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            // Replace the matched group with its uppercase counterpart
            matcher.appendReplacement(result, matcher.group(2).toUpperCase());
        }
        matcher.appendTail(result);

        // Optional: Ensure the first letter is lowercase for strict camelCase
        return Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }
}
