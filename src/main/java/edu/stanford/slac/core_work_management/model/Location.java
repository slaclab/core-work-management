package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Location {
    @Id
    String id;
    /**
     * The domain id where the location belong to
     */
    @Field(targetType = FieldType.OBJECT_ID)
    String domainId;
    /**
     * The parent location id
     */
    @Field(targetType = FieldType.OBJECT_ID)
    @Builder.Default
    String parentId = null;
    /**
     * The name of the location
     */
    String name;
    /**
     * The description of the location
     */
    String description;
    /**
     * The external location identifier
     * if an external location is used, the name and description
     * will be copied by the external location found by this information
     */
    String externalLocationIdentifier;
    /**
     * The location manager user id
     * is the user that is responsible for the location and need to
     * review to work done in that location
     */
    String locationManagerUserId;
    @CreatedDate
    private LocalDateTime createdDate;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    @LastModifiedBy
    private String lastModifiedBy;
    @Version
    private Long version;
}
