package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
import edu.stanford.slac.core_work_management.model.WorkLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the activity for work plan")
public record ActivityDTO(
        @Schema(description = "The unique identifier of the work plan")
        String id,
        @Schema(description = "The unique identifier of the work which this activity belongs to")
        String workId,
        @Schema(description = "The title of the activity")
        String title,
        @Schema(description = "The description of the activity")
        String description,
        @Schema(description = "The description of the test plan for the activity. This field provides a detailed description of the test plan associated with the activity.")
        String testPlanDescription,
        @Schema(description = "The description of the backout plan for the activity. This field provides a detailed description of the backout plan associated with the activity.")
        String backoutPlanDescription,
        @Schema(description = "The description of the system requirements for the activity. This field provides a detailed description of the system requirements associated with the activity.")
        String systemRequiredDescription,
        @Schema(description = "The description of the system effects for the activity. This field provides a detailed description of the system effects associated with the activity.")
        String systemEffectedDescription,
        @Schema(description = "The description of the risk and benefits for the activity. This field provides a detailed description of the risk and benefits associated with the activity.")
        String riskBenefitDescription,
        @Schema(description = "The description of the dependencies for the activity. This field provides a detailed description of the dependencies associated with the activity.")
        String dependenciesDescription,
        @Schema(description = "The type of the activity")
        ActivityTypeDTO activityType,
        @Schema(description = "The current status of the activity")
        ActivityStatusLogDTO currentStatus,
        @Schema(description = "The full activity status history")
        List<ActivityStatusLogDTO> statusHistory,
        @Schema(description = "The created date of the activity")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime createdDate,
        @Schema(description = "The user that created the activity")
        String createdBy,
        @Schema(description = "The last modified date of the activity")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the activity")
        String lastModifiedBy,
        Long version
) {
}
