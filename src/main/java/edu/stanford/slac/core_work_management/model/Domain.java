package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
/**
 * This class is used to represent the domain of the application
 * the domain is the main entity that is used to group the different work, location and shop groups
 */
public class Domain {
    @Id
    String id;
    String name;
    String description;
}
