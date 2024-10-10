package edu.stanford.slac.core_work_management.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.model.Attachment;
import edu.stanford.slac.core_work_management.service.AttachmentService;
import edu.stanford.slac.core_work_management.service.DocumentGenerationService;
import org.apache.kafka.clients.admin.AdminClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AttachmentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private DocumentGenerationService documentGenerationService;
    @Autowired
    private CWMAppProperties cwmAppProperties;
    @Autowired
    private KafkaAdmin kafkaAdmin;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.getCollection("fs.files").deleteMany(new Document());
        mongoTemplate.getCollection("fs.chunks").deleteMany(new Document());
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
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
    public void createAttachment() throws Exception {
        ApiResultResponse<String> newAttachmentID = testControllerHelperService.attachmentControllerCreateNew(
                mockMvc,
                status().isCreated(),
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                new MockMultipartFile(
                        "uploadFile",
                        "contract.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                )
        );

        Attachment retrivedAttachment = mongoTemplate.findOne(
                new Query().addCriteria(
                        Criteria.where("id").is(newAttachmentID.getPayload())
                ),
                Attachment.class
        );

        AssertionsForClassTypes.assertThat(retrivedAttachment).isNotNull();
    }
}
