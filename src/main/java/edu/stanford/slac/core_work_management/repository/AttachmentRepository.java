package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for the attachment managements
 */
public interface AttachmentRepository  extends MongoRepository<Attachment, String>, AttachmentRepositoryCustom {
}
