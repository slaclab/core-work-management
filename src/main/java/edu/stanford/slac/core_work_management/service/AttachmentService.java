package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.StorageObjectDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.AttachmentMapper;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.exception.AttachmentNotFound;
import edu.stanford.slac.core_work_management.model.Attachment;
import edu.stanford.slac.core_work_management.model.FileObjectDescription;
import edu.stanford.slac.core_work_management.model.StorageObject;
import edu.stanford.slac.core_work_management.repository.AttachmentRepository;
import edu.stanford.slac.core_work_management.repository.StorageRepository;
import io.micrometer.core.instrument.Counter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;


@Log4j2
@Service
@AllArgsConstructor
public class AttachmentService {
    final private AttachmentMapper attachmentMapper;
    final private CWMAppProperties cwmAppProperties;
    final private StorageRepository storageRepository;
    final private AttachmentRepository attachmentRepository;
    final private KafkaTemplate<String, Attachment> attachmentKafkaTemplate;
    final private Counter previewSubmittedCounter;

    /**
     * Create a new attachment
     * the input stream is closed directly in this method
     * @param attachment the new attachment content
     * @return the id of the new created attachment
     */
    @Transactional
    public String createAttachment(StorageObjectDTO attachment, boolean createPreview) {
        String resultId = null;
        try (StorageObject toSave = attachmentMapper.toModel(attachment)) {
            // store the object into storage
            String storedObjectId = wrapCatch(
                    () -> storageRepository.addObject(
                            toSave
                    ),
                    -1,
                    "AttachmentService::createAttachment"
            );
            // create the attachment
            Attachment newAttachmentCreated =
                    wrapCatch(
                            () -> attachmentRepository.insert(
                                    Attachment
                                            .builder()
                                            .fileName(toSave.getFilename())
                                            .contentType(toSave.getContentType())
                                            .originalId(storedObjectId)
                                            .build()
                            ),
                            0,
                            "AttachmentService::createAttachment");

            if (createPreview) {
                attachmentKafkaTemplate.send(cwmAppProperties.getImagePreviewTopic(), newAttachmentCreated);
                previewSubmittedCounter.increment();
            }
            resultId = newAttachmentCreated.getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("New attachment created with id {}", resultId);
        return resultId;
    }

    boolean exists(String id) {
        return wrapCatch(
                () -> attachmentRepository.existsById(
                        id
                ),
                0,
                "AttachmentService::exists");
    }

    /**
     * Return the attachment raw content file
     *
     * @param id the unique id of the attachment
     */
    public StorageObjectDTO getAttachmentContent(String id) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachmentContent"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getAttachmentContent")
                        .build()
        );
        return wrapCatch(
                () -> attachmentMapper.toDTO(storageRepository.getObject(foundAttachment.getOriginalId())),
                -1,
                "AttachmentService::getAttachmentContent"
        );
    }

    /**
     * return the preview content
     *
     * @param id the id of the attachment
     * @return the preview content
     */
    public StorageObjectDTO getPreviewContent(String id) {
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getPreviewContent"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getPreviewContent")
                        .build()
        );
        return wrapCatch(
                () -> attachmentMapper.toDTO(storageRepository.getObject(foundAttachment.getPreviewId())),
                -1,
                "AttachmentService::getPreviewContent"
        );
    }

    /**
     * Return the mini preview object description
     *
     * @param id the unique identifier of the attachment
     * @return the object stream of the mini preview
     */
    public FileObjectDescription getMiniPreviewContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getMiniPreviewContent"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getMiniPreviewContent")
                        .build()
        );

        // retrieve stored mini preview from model
        attachment.setFileName(foundAttachment.getFileName());
        attachment.setIs(new ByteArrayInputStream(foundAttachment.getMiniPreview()));
        attachment.setContentType(MediaType.IMAGE_JPEG_VALUE);
        return attachment;
    }

    /**
     * Return the attachment dto
     *
     * @param id the attachment id
     * @return the attachment dto
     */
    public AttachmentDTO getAttachment(String id) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getAttachment")
                        .build()
        );
        return attachmentMapper.fromModel(
                foundAttachment
        );
    }

    /**
     * Complete the attachment with the preview
     *
     * @param id        the id of the attachment
     * @param previewID the preview identifier for fetch it from object store
     */
    public void completePreview(String id, String previewID, byte[] miniPreview) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewID"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setPreviewID")
                        .build()
        );
        foundAttachment.setPreviewId(previewID);
        foundAttachment.setMiniPreview(miniPreview);
        foundAttachment.setPreviewState(Attachment.PreviewProcessingState.Completed);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setPreviewProcessingState"
        );
        log.info("Set the preview id to {} for the attachment {}", previewID, foundAttachment.getId());
    }

    /**
     * Update the processing state of the attachment
     *
     * @param id              the unique identifier of the attachment
     * @param processingState the new state of the attachment
     */
    public void setPreviewProcessingState(String id, Attachment.PreviewProcessingState processingState) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewProcessingState"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setPreviewProcessingState")
                        .build()
        );
        foundAttachment.setPreviewState(processingState);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setPreviewProcessingState"
        );
        log.info("Update the preview processing state to {} for the attachment {}", processingState, foundAttachment.getId());
    }

    /**
     * Return the processing state of the attachment
     *
     * @param id the unique id of the attachment
     * @return The string that represent the processing state
     */
    public String getPreviewProcessingState(String id) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getPreviewProcessingState"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getPreviewProcessingState")
                        .build()
        );
        return foundAttachment.getPreviewState().name();
    }

    /**
     * Set the mini preview of an attachment
     *
     * @param id        the unique identifier of an attachment
     * @param byteArray the byte array represent the mini preview
     */
    public void setMiniPreview(String id, byte[] byteArray) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setMiniPreview"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setMiniPreview")
                        .build()
        );

        foundAttachment.setMiniPreview(byteArray);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setMiniPreview"
        );
        log.info("Set the mini preview for the attachment {}", foundAttachment.getId());
    }

    /**
     * Set the in use flag of an attachment
     *
     * @param attachmentID the attachment id
     * @param inUse        the 'in use' flag
     * @return true
     */
    public Boolean setInUse(String attachmentID, boolean inUse) {
        wrapCatch(
                () -> {
                    attachmentRepository.setInUseState(
                            attachmentID,
                            inUse
                    );
                    return null;
                },
                -1,
                "AttachmentService::setInUse"
        );
        return true;
    }
}
