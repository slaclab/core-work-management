package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("string-value")
@AllArgsConstructor
@SuperBuilder
@ToString
public class StringValue extends AbstractValue {
    private String value;
}
