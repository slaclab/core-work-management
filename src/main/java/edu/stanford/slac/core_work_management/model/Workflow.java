package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Workflow {
    String id;
    String implementation;
}
