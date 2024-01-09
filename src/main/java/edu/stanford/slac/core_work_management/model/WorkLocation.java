package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a location where a work is to be carried out. The WorkLocation class provides
 * a generalized representation of a location by encapsulating its identifier, name, and
 * the system where this location is managed. This class is particularly useful in scenarios
 * where the location is managed by a different backend system, allowing for the integration
 * and identification of external location systems.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class WorkLocation {

    /**
     * The unique identifier for the location.
     * This ID can correspond to a location within the system or an external system, providing
     * a flexible reference.
     */
    private String id;

    /**
     * The name of the location.
     * This field stores the human-readable name or title of the location, aiding in easy
     * identification and reference.
     */
    private String name;

    /**
     * The name of the external system managing this location.
     * If the location is managed by an external backend system, this field contains the
     * name of that system. It is used to identify and integrate with different location
     * management systems.
     */
    private String systemName;
}
