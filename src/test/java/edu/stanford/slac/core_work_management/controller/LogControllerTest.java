package edu.stanford.slac.core_work_management.controller;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.elog_api.api.EntriesControllerApi;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "elog-support"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LogControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    HelperService helperService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    LocationService locationService;
    @Autowired
    AuthService authService;
    @Autowired
    DomainService domainService;
    @Autowired
    WorkService workService;
    @Autowired
    LOVService lovService;
    @Autowired
    TestControllerHelperService testControllerHelperService;
    @Autowired
    DocumentGenerationService documentGenerationService;
    @Autowired
    EntriesControllerApi entriesControllerApi;
    @Autowired
    AppProperties appProperties;
    @Autowired
    CWMAppProperties cwmAppProperties;
    @Autowired
    KafkaAdmin kafkaAdmin;

    private DomainDTO domain;
    private WorkflowDTO workflowDTO;
    private List<String> workIds;
    private String shopGroupId;
    private String locationId;
    private String newWorkTypeId;
    private String newWorkId;
    private String newActivityId;
    private List<LOVElementDTO> projectLovValues;
    @BeforeAll
    public void setUpWorkAndJob() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        // create domain
        domain = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("SLAC")
                                .description("SLAC National Accelerator Laboratory")
                                .workflowImplementations(Set.of("DummyParentWorkflow"))
                                .build()
                )
        );
        assertThat(domain).isNotNull();
        // get the workflow ID
        workflowDTO = domain.workflows().stream().findFirst().orElse(null);
        assertThat(workflowDTO).isNotNull();

        // create test work
        workIds = helperService.ensureWorkAndActivitiesTypes(
                domain.id(),
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .workflowId(workflowDTO.id())
                        .validatorName("validation/DummyParentValidation.groovy")
                        .build(),
                emptyList()
        );
        assertThat(workIds).hasSize(1);

        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domain.id(),
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .users(
                                                ImmutableSet.of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user2@slac.stanford.edu")
                                                                .build(),
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user3@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(shopGroupId).isNotEmpty();

        locationId =
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                domain.id(),
                                NewLocationDTO.builder()
                                        .name("SLAC")
                                        .description("SLAC National Accelerator Laboratory")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();

        newWorkTypeId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domain.id(),
                        NewWorkTypeDTO
                                .builder()
                                .title("Fix the hardware")
                                .description("Fix the hardware description")
                                .validatorName("validation/DummyParentValidation.groovy")
                                .workflowId(workflowDTO.id())
                                .build()
                )
        );

        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        newWorkId =
                assertDoesNotThrow(
                        () -> workService.createNew(
                                domain.id(),
                                NewWorkDTO.builder()
                                        .locationId(locationId)
                                        .workTypeId(newWorkTypeId)
                                        .shopGroupId(shopGroupId)
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkId).isNotEmpty();

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

    @BeforeEach
    void clenUp() {
        mongoTemplate.remove(new Query(), LogEntry.class);
    }

    @Test
    public void testCreateMewLogEntryOnWork() {
        Faker faker = new Faker();
        try (
                InputStream isPng = assertDoesNotThrow(() -> documentGenerationService.getTestPng());
                InputStream isJpg = assertDoesNotThrow(() -> documentGenerationService.getTestJpeg())
        ) {
            ApiResultResponse<Boolean> uploadResult = assertDoesNotThrow(
                    () -> testControllerHelperService.createLogEntry(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            domain.id(),
                            newWorkId,
                            NewLogEntry
                                    .builder()
                                    .title("second test entry from cwm")
                                    .text("second test entry from cwm")
                                    .build(),
                            new MockMultipartFile(
                                    "files",
                                    "test.png",
                                    MediaType.IMAGE_PNG_VALUE,
                                    isPng
                            ),
                            new MockMultipartFile(
                                    "files",
                                    "test.jpg",
                                    MediaType.IMAGE_JPEG_VALUE,
                                    isJpg
                            ))
            );
            // Process the uploadResult as needed
            assertThat(uploadResult).isNotNull();
            assertThat(uploadResult.getPayload()).isTrue();

            //try to fetch the log entry using elog api
            var fullWork = workService.findWorkById(domain.id(), newWorkId, WorkDetailsOptionDTO.builder().build());
            await()
                    .atMost(30, HOURS)
                    .pollDelay(2, SECONDS)
                    .until(() -> {
                        var result = entriesControllerApi.search(
                                null,
                                null,
                                null,
                                null,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "cwm:work:%s".formatted(fullWork.workNumber())
                        );
                        return result != null &&
                                result.getErrorCode() == 0 &&
                                result.getPayload() != null &&
                                !result.getPayload().isEmpty();
                    });
        } catch (Exception e) {
            // Handle possible exceptions here
        }
    }


    @Test
    public void testCreateMewLogOnWorkCreation() {
        var newWorkLogIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domain.id(),
                        NewWorkDTO.builder()
                                .locationId(locationId)
                                .workTypeId(newWorkTypeId)
                                .shopGroupId(shopGroupId)
                                .title("work contextually to log creation")
                                .description("this is a work that will be used to test log creation during the work creation")
                                .build(),
                        Optional.of(true)
                )
        );


        //try to fetch the log entry using elog api
        var fullWork = workService.findWorkById(domain.id(), newWorkLogIdResult.getPayload(), WorkDetailsOptionDTO.builder().build());
        await()
                .atMost(30, SECONDS)
                .pollDelay(2, SECONDS)
                .pollInterval(2, SECONDS)
                .until(() -> {
                    var result = entriesControllerApi.search(
                            null,
                            null,
                            null,
                            null,
                            10,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "cwm:work:%s".formatted(fullWork.workNumber())
                    );
                    return result != null &&
                            result.getErrorCode() == 0 &&
                            result.getPayload() != null &&
                            !result.getPayload().isEmpty();
                });

    }
}
