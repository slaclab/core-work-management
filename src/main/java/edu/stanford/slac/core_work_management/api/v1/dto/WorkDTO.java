package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import edu.stanford.slac.core_work_management.model.WorkLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for create new work plan")
public record WorkDTO(
        @Schema(description = "The unique identifier of the work plan")
        String id,
        @Schema(description = "The unique identifier of the work which his is related to")
        String relatedToWorkId,
        @Schema(description = "The type of the work")
        WorkTypeDTO workType,
        @Schema(description = "The current status of the work")
        WorkStatusLogDTO currentStatus,
        @Schema(description = "The full work status history")
        List<WorkStatusLogDTO> statusHistory,
        @Schema(description = "The title of the work")
        String title,
        @Schema(description = "The description of the work")
        String description,
        @Schema(description = "The list of the user that are assigned to the work")
        List<String> assignedTo,
        @Schema(description = "The location of the work, if any")
        WorkLocation location,
        @Schema(description = "The shop group that perform the work in the location")
        ShopGroupDTO shopGroup,
        @Schema(description = "The created date of the work")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime createdDate,
        @Schema(description = "The user that created the work")
        String createdBy,
        @Schema(description = "The last modified date of the work")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the work")
        String lastModifiedBy,
        Long version
) {
}
