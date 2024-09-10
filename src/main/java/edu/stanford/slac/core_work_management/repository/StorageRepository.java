package edu.stanford.slac.core_work_management.repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import edu.stanford.slac.core_work_management.exception.AttachmentNotFound;
import edu.stanford.slac.core_work_management.model.StorageObject;
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

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Repository
public class StorageRepository {
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
     * Add an object storage for an attachment
     * that can have only one object stored
     *
     * @param storageObject The storage object
     * @return The id of the attachment
     * @throws IOException If the file is not found
     */
    public String addObject(StorageObject storageObject) throws IOException {
        DBObject metaData = new BasicDBObject();
        metaData.put("type", "attachment");
        metaData.put("fileName", storageObject.getFilename());

        ObjectId id = gridFsTemplate
                .store
                        (
                                storageObject.getFile(),
                                storageObject.getFilename(),
                                storageObject.getContentType(),
                                metaData
                        );
        return id.toString();
    }

    /**
     * Get the attachment by id
     *
     * @param id The id of the attachment
     * @return The attachment
     */
    public StorageObject getObject(String id) throws IllegalStateException, IOException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        assertion(
                AttachmentNotFound
                        .attachmentNotFoundBuilder()
                        .attachmentID(id)
                        .errorCode(-1)
                        .errorDomain("StorageRepository::getAttachment")
                        .build(),
                () -> file != null
        );
        var resFile = operations.getResource(file);
        return StorageObject.builder()
                .filename(resFile.getFilename())
                .contentType(resFile.getContentType())
                .file(resFile.getInputStream())
                .build();
    }

    /**
     * Get the object by attachment id
     *
     * @param attachmentId The id of the attachment
     * @return The object
     */
    public StorageObject getObjectByAttachmentId(String attachmentId) throws IOException {
        List<String> ids = new ArrayList<>();
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("metadata.attachmentId").is(attachmentId)));
        assertion(
                AttachmentNotFound
                        .attachmentNotFoundBuilder()
                        .attachmentID(attachmentId)
                        .errorCode(-1)
                        .errorDomain("StorageRepository::getObjectByAttachmentId")
                        .build(),
                () -> file != null
        );
        var resFile = operations.getResource(file);
        return StorageObject.builder()
                .filename(resFile.getFilename())
                .contentType(resFile.getContentType())
                .file(resFile.getInputStream())
                .build();
    }

}
