package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
/**
 * This class is used to represent the domain of the application
 * the domain is the main entity that is used to group the different work, location and shop groups
 */
public class Domain {

    /**
     * The unique identifier for the domain.
     * This field is annotated with @Id to indicate that it's the primary key.
     */
    @Id
    private String id;
    /**
     * The name of the domain.
     * This field stores the name of the domain, using @Schema annotation to provide a description for the field.
     */
    private String name;
    /**
     * The description of the domain.
     * This field stores the description of the domain, using @Schema annotation to provide a description for the field.
     */
    private String description;

    /**
     * The list of the work types associated with the domain.
     */
    private Set<Workflow> workflows;

    /**
     * The work type status statistics of the domain.
     * this field contains the statistics of the work by work type id
     * the key of the map is the work type id and the value is the list of the status statistics
     */
    private Map<String, List<WorkStatusCountStatistics>> workTypeStatusStatistics;

    /**
     * The date when the domain was created.
     * This field is annotated with @CreatedDate to indicate that it stores the date when the domain was created.
     */
    @CreatedDate
    private LocalDateTime createdDate;

    /**
     * The user who created the domain.
     * This field is annotated with @CreatedBy to indicate that it stores the user who created the domain.
     */
    @CreatedBy
    private String createdBy;

    /**
     * The date when the domain was last modified.
     * This field is annotated with @LastModifiedDate to indicate that it stores the date when the domain was last modified.
     */
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    /**
     * The user who last modified the domain.
     * This field is annotated with @LastModifiedBy to indicate that it stores the user who last modified the domain.
     */
    @LastModifiedBy
    private String lastModifiedBy;
}
