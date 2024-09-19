package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("bucket-value")
@AllArgsConstructor
@SuperBuilder
@ToString
public class BucketValue extends AbstractValue {
    // the id of the bucket
    private String value;
}
