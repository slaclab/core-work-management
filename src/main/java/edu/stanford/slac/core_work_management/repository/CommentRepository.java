package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByRelatedDocumentId(String relatedDocumentId);
}
