/*
 * -----------------------------------------------------------------------------
 * Title      : WorkQueryParameter
 * ----------------------------------------------------------------------------
 * File       : WorkQueryParameter.java
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

import edu.stanford.slac.core_work_management.api.v1.dto.WorkflowStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class WorkQueryParameter {
    private List<String> domainIds;
    private List<String> workTypeIds;
    private String anchorID;
    private Integer contextSize;
    private Integer limit;
    private String search;
    List<String> createdBy;
    List<String> assignedTo;
    List<String> workflowName;
    List<WorkflowStateDTO> workflowState;
}
