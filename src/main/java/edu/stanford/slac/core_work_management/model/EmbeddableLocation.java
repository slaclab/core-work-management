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
public class EmbeddableLocation {
    @Id
    String id;
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
}
