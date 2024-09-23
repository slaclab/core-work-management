package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository for the attachment managements
 */
public interface AttachmentRepository  extends MongoRepository<Attachment, String>, AttachmentRepositoryCustom {
    /**
     * Check if all the attachments with the given ids exists
     * @param ids the list of ids to check
     * @return true if all the attachments exists, false otherwise
     */
    boolean existsAllByIdIn(List<String> ids);
}
