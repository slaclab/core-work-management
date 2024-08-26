package edu.stanford.slac.core_work_management.controller;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.elog_api.api.EntriesControllerApi;
import edu.stanford.slac.core_work_management.migration.M1004_InitProjectLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
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

    private String domainId;
    private List<String> workActivityIds;
    private String shopGroupId;
    private String locationId;
    private String newWorkTypeId;
    private String newWorkId;
    private String newActivityId;
    private List<LOVElementDTO> projectLovValues;
    @BeforeAll
    public void setUpWorkAndJob() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        // crete lov for 'project' static filed
        M1004_InitProjectLOV m1004_initProjectLOV = new M1004_InitProjectLOV(lovService);
        assertDoesNotThrow(m1004_initProjectLOV::changeSet);
        projectLovValues = assertDoesNotThrow(()->lovService.findAllByGroupName("Project"));

        // create domain
        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("SLAC")
                                .description("SLAC National Accelerator Laboratory")
                                .build()
                )
        );

        // create test work
        workActivityIds = helperService.ensureWorkAndActivitiesTypes(
                domainId,
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                emptyList()
        );
        assertThat(workActivityIds).hasSize(2);

        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .domainId(domainId)
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
                                NewLocationDTO
                                        .builder()
                                        .domainId(domainId)
                                        .name("SLAC")
                                        .description("SLAC National Accelerator Laboratory")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();

        newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );

        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        newWorkId =
                assertDoesNotThrow(
                        () -> workService.createNew(
                                NewWorkDTO.builder()
                                        .domainId(domainId)
                                        .locationId(locationId)
                                        .workTypeId(newWorkTypeId)
                                        .shopGroupId(shopGroupId)
                                        .project(projectLovValues.get(0).id())
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkId).isNotEmpty();

        // crea
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
            var fullWork = workService.findWorkById(newWorkId, WorkDetailsOptionDTO.builder().build());
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
                        NewWorkDTO.builder()
                                .domainId(domainId)
                                .locationId(locationId)
                                .workTypeId(newWorkTypeId)
                                .shopGroupId(shopGroupId)
                                .title("work contextually to log creation")
                                .description("this is a work that will be used to test log creation during the work creation")
                                .project(projectLovValues.get(0).id())
                                .build(),
                        Optional.of(true)
                )
        );


        //try to fetch the log entry using elog api
        var fullWork = workService.findWorkById(newWorkLogIdResult.getPayload(), WorkDetailsOptionDTO.builder().build());
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

    @Test
    public void testCreateMewLogEntryOnActivity() {
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
                                "cwm:work:%s".formatted(newWorkId)
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
}
