package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

/**
 * ActivityType model
 *
 */

@AllArgsConstructor
public enum ActivityTypeSubtype {
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
