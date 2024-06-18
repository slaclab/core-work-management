package edu.stanford.slac.core_work_management.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import edu.stanford.slac.core_work_management.model.value.LOVField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BucketSlot {
    @Id
    private String id;
    private String description;
    /**
     * The id of the lov value used to define the bucket type
     */
    @LOVField(fieldReference = "bucketType", isMandatory = true)
    /**
     * The id of the lov value used to define the bucket status
     */
    private String bucketType;
    @LOVField(fieldReference = "bucketStatus", isMandatory = true)
    private String bucketStatus;
    private LocalDateTime from;
    private LocalDateTime to;
    @CreatedDate
    LocalDateTime createdDate;
    @CreatedBy
    String createdBy;
    @LastModifiedDate
    LocalDateTime lastModifiedDate;
    @LastModifiedBy
    String lastModifiedBy;
    @Version
    Long version;
}
