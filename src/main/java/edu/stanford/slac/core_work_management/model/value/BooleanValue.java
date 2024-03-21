package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@ToString
@JsonTypeName("bool-value")
public class BooleanValue extends AbstractValue {
    private Boolean value;
}

