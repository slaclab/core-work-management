package edu.stanford.slac.core_work_management.model;

import lombok.*;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessWorkflowInfo {
    String domainId;
    String workId;
}
