package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("lov-value")
@AllArgsConstructor
@SuperBuilder
@ToString
public class LOVValue extends AbstractValue {
    private String value;
}
