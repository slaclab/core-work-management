package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationFilter {
    public String text;
}
