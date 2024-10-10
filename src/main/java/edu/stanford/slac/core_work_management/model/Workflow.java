package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Workflow implements Serializable {
    String id;
    String implementation;
}
