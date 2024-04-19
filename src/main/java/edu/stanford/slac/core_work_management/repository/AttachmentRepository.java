package edu.stanford.slac.core_work_management.repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import edu.stanford.slac.core_work_management.model.Attachment;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AttachmentRepository {
    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations operations;

    /**
     * Add an attachment to the log entry
     *
     * @param relationId  The custom id of the attachment that permit to create it to any other resources
     * @param title       The title of the attachment
     * @param file        The file to be attached
     * @param name        The name of the file
     * @param contentType The content type of the file
     * @return The id of the attachment
     */
    public String addAttachment(String relationId, String title, InputStream file, String name, String contentType) throws IOException {
        DBObject metaData = new BasicDBObject();
        metaData.put("type", "attachment");
        metaData.put("relationId", relationId);
        metaData.put("title", title);
        ObjectId id = gridFsTemplate.store(
                file, name, contentType, metaData);
        return id.toString();
    }

    /**
     * Get the attachment by id
     *
     * @param id The id of the attachment
     * @return The attachment
     */
    public Attachment getAttachment(String id) throws IllegalStateException, IOException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        var resFile = operations.getResource(file);
        return Attachment.builder()
                .filename(resFile.getFilename())
                .contentType(resFile.getContentType())
                .file(resFile.getInputStream())
                .build();
    }

    /**
     * Get the attachments by custom id
     *
     * @param relationId The custom id of the attachment
     * @return The list of the attachments
     */
    public List<String> getAttachmentsByRelationId(String relationId) {
        List<String> ids = new ArrayList<>();
        gridFsTemplate.find(new Query(Criteria.where("metadata.relationId").is(relationId)))
                .forEach(file -> ids.add(file.getObjectId().toString()));
        return ids;
    }
}
