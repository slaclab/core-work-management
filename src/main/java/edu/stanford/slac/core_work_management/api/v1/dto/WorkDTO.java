package edu.stanford.slac.core_work_management.api.v1.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangesHistoryDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.core_work_management.model.WorkBucketAssociation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Define the information for create new work plan")
public record WorkDTO(
        @Schema(description = "The unique identifier of the work plan")
        String id,
        @Schema(description = "The parent work id if the work is a sub work")
        String parentWorkId,
        @Schema(description = "The domain where the work belongs to")
        DomainDTO domain,
        @Schema(description = "The unique identifier of the work plan")
        Long workNumber,
        @Schema(description = "The unique identifier of the work which his is related to")
        List<String> relatedToWorkIds,
        @Schema(description = "The type of the work")
        EmbeddableWorkTypeDTO workType,
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
        LocationDTO location,
        @Schema(description = "The shop group that perform the work in the location")
        ShopGroupDTO shopGroup,
        @Schema(description = "The list of the custom fields associated with the work. The custom fields are used to store additional information about the specific work type.")
        List<CustomFieldDTO> customFields,
        @Schema(
                description = "The list of the attachment id to associate to the work",
                type = "array",
                additionalItems = String.class)
        List<String> attachments,
        @Schema(description = "The list of changes on the work, each element represent a change on the work for a single save operation")
        List<ModelChangesHistoryDTO> changesHistory,
        @Schema(description = "The list of the bucket association for the work")
        WorkBucketAssociationDTO currentBucketAssociation,
        @Schema(description = "The list of the bucket association for the work")
        List<WorkBucketAssociationDTO> bucketAssociationsHistory,
        @Schema(description = "The list of the user that are watching the work")
        Set<PersonDTO> userWatchlist,
        @Schema(description = "Identify if the work has a log")
        Boolean hasLog,
        @Schema(description = "The created date of the work")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime createdDate,
        @Schema(description = "The user that created the work")
        PersonDTO createdBy,
        @Schema(description = "The last modified date of the work")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime lastModifiedDate,
        @Schema(description = "The user that last modified the work")
        PersonDTO lastModifiedBy,
        Long version,
        @Schema(description = "The authorization access for the work specific resources")
        List<AuthorizationResourceDTO> accessList
) {
}
