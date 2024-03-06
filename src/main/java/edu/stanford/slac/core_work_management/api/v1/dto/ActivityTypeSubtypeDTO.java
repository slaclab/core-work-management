package edu.stanford.slac.core_work_management.api.v1.dto;

import lombok.AllArgsConstructor;

/**
 * ActivityType model
 *
 */

@AllArgsConstructor
public enum ActivityTypeSubtypeDTO {
    BugFix,
    DeferredRepair,
    Enhancement,
    Fabrication,
    Inspection,
    Installation,
    Maintenance,
    NewApplication,
    Other,
    Safety,
    SoftwareRelease
}
