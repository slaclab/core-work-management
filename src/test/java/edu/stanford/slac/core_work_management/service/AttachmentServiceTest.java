package edu.stanford.slac.core_work_management.service;


import edu.stanford.slac.core_work_management.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.StorageObjectDTO;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.model.Attachment;
import edu.stanford.slac.core_work_management.repository.AttachmentRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.assertj.core.api.AssertionsForClassTypes;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AttachmentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceTest.class);
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private DocumentGenerationService documentGenerationService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private KafkaAdmin kafkaAdmin;
    @Autowired
    private CWMAppProperties cwmAppProperties;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(Attachment.class).all();
        mongoTemplate.getCollection("fs.files").deleteMany(new Document());
        mongoTemplate.getCollection("fs.chunks").deleteMany(new Document());
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            List<String> topicsToDelete = List.of(
                    cwmAppProperties.getImagePreviewTopic(),
                    String.format("%s-retry-2000", cwmAppProperties.getImagePreviewTopic()),
                    String.format("%s-retry-4000", cwmAppProperties.getImagePreviewTopic())
            );

            // Delete topics that actually exist
            topicsToDelete.stream()
                    .filter(existingTopics::contains)
                    .forEach(topic -> {
                        try {
                            adminClient.deleteTopics(Collections.singletonList(topic)).all().get();
                        } catch (Exception e) {
                            System.err.println("Failed to delete topic " + topic + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to recreate Kafka topic", e);
        }
    }

    @Test
    public void testPreviewJpegOk() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestJpeg()
        )) {
            //save the
            String attachmentID = assertDoesNotThrow(
                    () -> attachmentService.createAttachment(
                            StorageObjectDTO
                                    .builder()
                                    .filename("jpegFileName")
                                    .contentType(MediaType.IMAGE_JPEG_VALUE)
                                    .file(is)
                                    .build(),
                            true
                    )
            );

            await()
                    .atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                            () -> {
                                String state = attachmentService.getPreviewProcessingState(attachmentID);
                                log.info("state {} for attachment id {}", state, attachmentID);
                                return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                            }
                    );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            AssertionsForClassTypes.assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());

            var attachmentModel = attachmentRepository.findById(attachmentID);
            AssertionsForClassTypes.assertThat(attachmentModel.isPresent()).isTrue();
            AssertionsForClassTypes.assertThat(attachmentModel.get().getInUse()).isFalse();
        }
    }

    @Test
    public void testPreviewPNGOk() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestPng()
        )) {
            //save the
            String attachmentID = attachmentService.createAttachment(
                    StorageObjectDTO
                            .builder()
                            .filename("jpegFileName")
                            .contentType(MediaType.IMAGE_JPEG_VALUE)
                            .file(is)
                            .build(),
                    true
            );

            await()
                    .atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                    () -> {
                        String state = attachmentService.getPreviewProcessingState(attachmentID);
                        log.info("state {} for attachment id {}", state, attachmentID);
                        return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                    }
            );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            AssertionsForClassTypes.assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());
        }
    }

    @Test
    public void testPdfPreview() throws IOException {
        try (PDDocument pdf = assertDoesNotThrow(
                () -> documentGenerationService.generatePdf()
        )) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdf.save(baos);

            //save the
            String attachmentID = attachmentService.createAttachment(
                    StorageObjectDTO
                            .builder()
                            .filename("pdfFileName")
                            .contentType(MediaType.APPLICATION_PDF_VALUE)
                            .file(new ByteArrayInputStream(baos.toByteArray()))
                            .build(),
                    true
            );

            await().atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                            () -> {
                                String state = attachmentService.getPreviewProcessingState(attachmentID);
                                log.info("state {} for attachement id {}", state, attachmentID);
                                return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                            }
                    );
        }
    }

    @Test
    public void testPSPreview() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestPS()
        )) {
            //save the
            String attachmentID = attachmentService.createAttachment(
                    StorageObjectDTO
                            .builder()
                            .filename("psFileName")
                            .contentType("application/ps")
                            .file(is)
                            .build(),
                    true
            );

            await()
                    .atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                    () -> {
                        String state = attachmentService.getPreviewProcessingState(attachmentID);
                        log.info("state {} for attachment id {}", state, attachmentID);
                        return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                    }
            );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            AssertionsForClassTypes.assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());
        }
    }

    @Test
    public void testPSAlternateMimeTypePreview() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestPS()
        )) {
            //save the
            String attachmentID = attachmentService.createAttachment(
                    StorageObjectDTO
                            .builder()
                            .filename("psFileName")
                            .contentType("application/postscript")
                            .file(is)
                            .build(),
                    true
            );

            await()
                    .atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                            () -> {
                                String state = attachmentService.getPreviewProcessingState(attachmentID);
                                log.info("state {} for attachment id {}", state, attachmentID);
                                return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                            }
                    );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            AssertionsForClassTypes.assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());
        }
    }
}
