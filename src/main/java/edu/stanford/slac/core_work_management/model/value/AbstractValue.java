package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanValue.class, name = "bool-value"),
        @JsonSubTypes.Type(value = DateTimeValue.class, name = "date-time-value"),
        @JsonSubTypes.Type(value = DateValue.class, name = "date-value"),
        @JsonSubTypes.Type(value = NumberValue.class, name = "number-value"),
        @JsonSubTypes.Type(value = DoubleValue.class, name = "double-value"),
        @JsonSubTypes.Type(value = StringValue.class, name = "string-value"),
        @JsonSubTypes.Type(value = LOVValue.class, name = "lov-value"),
        @JsonSubTypes.Type(value = AttachmentsValue.class, name = "attachments-value")
})

public class AbstractValue {}
