package edu.stanford.slac.core_work_management.api.v1.mapper;


import edu.stanford.slac.core_work_management.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.StorageObjectDTO;
import edu.stanford.slac.core_work_management.model.Attachment;
import edu.stanford.slac.core_work_management.model.StorageObject;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class AttachmentMapper {
    public abstract AttachmentDTO fromModel(Attachment model);

    public abstract StorageObjectDTO toDTO(StorageObject attachment);

    public abstract StorageObject toModel(StorageObjectDTO object);
}
