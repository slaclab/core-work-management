package edu.stanford.slac.core_work_management.model;

import edu.stanford.slac.core_work_management.model.value.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

/**
 * ActivityType model
 *
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ActivityTypeCustomField {
    private String id;
    private String name;
    private String description;
    private ValueType valueType;
    private Boolean isLov;
    private Boolean isMandatory;
}
