package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;


@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the activity for work plan")
public record ActivityDTO(
        @Schema(description = "The unique identifier of the work plan")
        String id,
        @Schema(description = "The sequence number of the activity in the work plan")
        Integer activityNumber,
        @Schema(description = "The unique identifier of the work which this activity belongs to")
        String workId,
        @Schema(description = "The title of the activity")
        String title,
        @Schema(description = "The description of the activity")
        String description,
        @Schema(description = "The type of the activity")
        ActivityTypeDTO activityType,
        @Schema(description = "The subtype of the activity")
        ActivityTypeSubtypeDTO activityTypeSubtype,
        @Schema(description = "The priority of the activity")
        String schedulingProperty,
        @Schema(description="The list of the custom fields associated with the activity. The custom fields are used to store additional information about the activity.")
        List<CustomFieldDTO> customFields,
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
        @Schema(description = "The authorization access for the activity")
        AuthorizationTypeDTO access
) {}
