package edu.stanford.slac.core_work_management.controller.domain.tec;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import edu.stanford.slac.core_work_management.controller.domain.BaseWorkflowDomainTest;
import edu.stanford.slac.core_work_management.controller.domain.DomainTestInfo;
import edu.stanford.slac.core_work_management.model.Attachment;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.EventTrigger;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.task.ManageBucketWorkflowUpdate;
import edu.stanford.slac.core_work_management.task.ManageWorkflowUpdateByEventTrigger;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TecSoftwareRecordTest {
    @SpyBean
    private Clock clock; // Mock the Clock bean
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private ManageBucketWorkflowUpdate manageBucketWorkflowUpdate;
    @Autowired
    private ManageWorkflowUpdateByEventTrigger manageWorkflowUpdateByEventTrigger;
    @Autowired
    private BaseWorkflowDomainTest tecDomainEnvironmentTest;
    @Autowired
    private CWMAppProperties cwmAppProperties;
    @Autowired
    private KafkaAdmin kafkaAdmin;
    // test tec domain data
    private DomainTestInfo domainTestInfo = null;

    @BeforeAll
    public void init() {
        domainTestInfo = tecDomainEnvironmentTest.init();
        assertThat(domainTestInfo).isNotNull();
        assertThat(domainTestInfo.domain).isNotNull();
        assertThat(domainTestInfo.domain.id()).isNotNull().isNotEmpty();
        assertThat(domainTestInfo.lovElementBucketStatus).isNotEmpty();
        assertThat(domainTestInfo.lovElementBucketType).isNotEmpty();
    }

    @BeforeEach
    public void clear() {
        // clean the test domain info
        tecDomainEnvironmentTest.clean(domainTestInfo);

        // clean additional used data
        mongoTemplate.remove(Work.class).all();
        mongoTemplate.remove(Attachment.class).all();
        mongoTemplate.remove(EventTrigger.class).all();
        mongoTemplate.remove(BucketSlot.class).all();

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

        // reset the clock to be used to mock the advance of time
        Mockito.reset(clock);
    }


    @Test
    public void failingNoMandatoryField() {
        // create a new work
        var failForMandatoryField = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.getWorkTypeByName("Software Record").id())
                                .build()
                )
        );
        assertThat(failForMandatoryField).isNotNull();
        // check that message contains the needed field
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("title");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("description");
    }

}
