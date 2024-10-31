package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.CommentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewCommentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateCommentDTO;
import edu.stanford.slac.core_work_management.model.Comment;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class CommentMapping {
    @Autowired
    private PeopleGroupService peopleGroupService;

    abstract public Comment toModel(String relatedDocumentId, NewCommentDTO newCommentDTO);

    @Mapping(target = "createdBy", expression = "java(getPersonDTO(comment.getCreatedBy()))")
    abstract public CommentDTO toDTO(Comment comment);

    abstract public Comment updateComment(@MappingTarget Comment comment, @Valid UpdateCommentDTO updateWorkDTO);

    public PersonDTO getPersonDTO(String email) {
        if (email == null) return null;
        return peopleGroupService.findPersonByEMail(email);
    }
}
