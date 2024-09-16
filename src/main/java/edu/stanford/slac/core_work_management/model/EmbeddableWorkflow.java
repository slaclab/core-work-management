package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkflowStateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class EmbeddableWorkflow implements Serializable {
    private String id;
    private String name;
    private String implementation;
    private Map<WorkflowStateDTO, Set<WorkflowStateDTO>> validTransitions;
}
