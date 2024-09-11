package edu.stanford.slac.core_work_management.model.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("attachment-value")
@AllArgsConstructor
@SuperBuilder
@ToString
public class AttachmentsValue extends AbstractValue {
    // the id of the attachments
    private List<String> value;
}
