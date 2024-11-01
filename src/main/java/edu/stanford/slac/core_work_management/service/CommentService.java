package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.CommentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewCommentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateCommentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.CommentMapping;
import edu.stanford.slac.core_work_management.model.Comment;
import edu.stanford.slac.core_work_management.repository.CommentRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Service
@AllArgsConstructor
public class CommentService {
    private CommentMapping commentMapping;
    private CommentRepository commentRepository;

    /**
     * Create a new comment
     *
     * @param relatedDocumentID The related document id
     * @param newCommentDTO     The new comment DTO
     */
    public void createComment(String relatedDocumentID, @Valid NewCommentDTO newCommentDTO) {
        Comment comment = commentMapping.toModel(relatedDocumentID, newCommentDTO);
        wrapCatch(
                () -> commentRepository.save(comment),
                -1
        );
        log.info("Comment created on work {}", relatedDocumentID);
    }

    /**
     * Update a comment
     *
     * @param id            The comment id
     * @param updateCommentDTO the update to the comment
     */
    public void updateComment(String id, @Valid UpdateCommentDTO updateCommentDTO) {
        Comment comment = wrapCatch(
                () -> commentRepository.findById(id),
                -1
        ).orElseThrow(() -> ControllerLogicException.builder().build());

        var commentUpdated = wrapCatch(
                () -> commentRepository.save(commentMapping.updateComment(comment, updateCommentDTO)),
                -2
        );
        log.info("Comment updated on work {}", commentUpdated.getId());
    }

    /**
     * Find a comment for a related documentId
     *
     * @param relatedDocumentId The comment id
     */
    public List<CommentDTO> getCommentsByRelatedDocumentId(String relatedDocumentId) {
        return wrapCatch(
                () -> commentRepository.findByRelatedDocumentId(relatedDocumentId),
                -1
        ).stream().map(commentMapping::toDTO).toList();
    }
}
