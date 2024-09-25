package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import edu.stanford.slac.core_work_management.migration.M1000_InitLOV;
import edu.stanford.slac.core_work_management.migration.M1001_InitTECDomain;
import edu.stanford.slac.core_work_management.migration.M1003_InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.HelperService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.task.ManageBucketWorkflowUpdate;
import edu.stanford.slac.core_work_management.task.ManageWorkflowUpdateByEventTrigger;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TecHardwareReportTest extends BaseWorkflowDomainTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LOVService lovService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private KafkaAdmin kafkaAdmin;
    @Autowired
    private CWMAppProperties cwmAppProperties;
    @Autowired
    private ManageBucketWorkflowUpdate manageBucketWorkflowUpdate;
    @Autowired
    private ManageWorkflowUpdateByEventTrigger manageWorkflowUpdateByEventTrigger;

    // bucket lov
    private List<LOVElementDTO> bucketTypeLOVIds = null;
    private List<LOVElementDTO> bucketStatusLOVIds = null;
    // test tec domain
    private DomainDTO tecDomain = null;
    private List<WorkTypeDTO> workTypes = new ArrayList<>();
    private String hardwareRequestWorkTypeId = null;
    private String hardwareReportWorkTypeId = null;
    private String location10 = null;
    private String location20 = null;
    private String shopGroup15 = null;
    private String shopGroup17 = null;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(Domain.class).all();
        mongoTemplate.remove(WorkType.class).all();
        mongoTemplate.remove(LOVElement.class).all();
        mongoTemplate.remove(Location.class).all();
        mongoTemplate.remove(ShopGroup.class).all();
        mongoTemplate.remove(BucketSlot.class).all();

        // init general lov
        M1000_InitLOV initLOV = new M1000_InitLOV(lovService);
        assertDoesNotThrow(initLOV::initLOV);

        // init bucket type lov
        M1003_InitBucketTypeLOV initBucketTypeLOV = new M1003_InitBucketTypeLOV(lovService);
        assertDoesNotThrow(initBucketTypeLOV::changeSet);

        bucketTypeLOVIds = lovService.findAllByGroupName("BucketType");
        bucketStatusLOVIds = lovService.findAllByGroupName("BucketStatus");

        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        M1001_InitTECDomain initWorkType = new M1001_InitTECDomain(lovService, domainService);
        tecDomain = assertDoesNotThrow(initWorkType::initTECDomain);
        assertThat(tecDomain).isNotNull();
        // find all work type
        var workTypesResult = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerFindAllWorkTypes(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id()
                )
        );
        assertThat(workTypesResult).isNotNull();
        assertThat(workTypesResult.getErrorCode()).isEqualTo(0);
        assertThat(workTypesResult.getPayload()).isNotNull().isNotEmpty().hasSize(2);
        workTypes = workTypesResult.getPayload();
        hardwareRequestWorkTypeId = workTypes.stream().filter(wt -> wt.title().equals("Hardware Request")).findFirst().get().id();
        hardwareReportWorkTypeId = workTypes.stream().filter(wt -> wt.title().equals("Hardware Report")).findFirst().get().id();

        // create locations
        var location10Result = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewLocationDTO.builder()
                                .name("Location10")
                                .description("LocationA description")
                                .locationManagerUserId("user10@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(location10Result).isNotNull();
        assertThat(location10Result.getErrorCode()).isEqualTo(0);
        location10 = location10Result.getPayload();

        var location20Result = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewLocationDTO.builder()
                                .name("Location20")
                                .description("LocationA description")
                                .locationManagerUserId("user20@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(location20Result).isNotNull();
        assertThat(location20Result.getErrorCode()).isEqualTo(0);
        location20 = location20Result.getPayload();

        // add shop group
        var shopGroup15Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewShopGroupDTO.builder()
                                .name("shop15")
                                .description("shop15 description")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user15@slac.stanford.edu")
                                                        .isLeader(true)
                                                        .build(),
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user16@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(shopGroup15Result).isNotNull();
        assertThat(shopGroup15Result.getErrorCode()).isEqualTo(0);
        shopGroup15 = shopGroup15Result.getPayload();

        // create shop group 17
        var shopGroup17Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewShopGroupDTO.builder()
                                .name("shop17")
                                .description("shop17 description")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user17@slac.stanford.edu")
                                                        .isLeader(true)
                                                        .build(),
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user18@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(shopGroup17Result).isNotNull();
        assertThat(shopGroup17Result.getErrorCode()).isEqualTo(0);
        shopGroup17 = shopGroup17Result.getPayload();

        // create test bucket
        var bucketSlotResult = assertDoesNotThrow(
                () -> testControllerHelperService.maintenanceControllerCreateNewBucket(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user18@slac.stanford.edu"),
                        NewBucketDTO
                                .builder()
                                .description("test")
                                .domainIds(Set.of(tecDomain.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO
                                                        .builder()
                                                        .domainId(tecDomain.id())
                                                        .workTypeId(workTypes.getFirst().id())
                                                        .build()
                                        )
                                )
                                .type(bucketTypeLOVIds.get(0).id())
                                .status(bucketStatusLOVIds.get(0).id())
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 2, 23, 0))
                                .build()
                )
        );
        assertThat(bucketSlotResult).isNotNull();

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
    public void clear() {
        mongoTemplate.remove(Work.class).all();
        mongoTemplate.remove(Attachment.class).all();
        mongoTemplate.remove(EventTrigger.class).all();
    }


    @Test
    public void hardwareRequestFailingNoMandatoryField() {
        // create a new work
        var failForMandatoryField = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(hardwareRequestWorkTypeId)
                                .build()
                )
        );
        assertThat(failForMandatoryField).isNotNull();
        // check that message contains the needed field
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("title");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("description");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("location");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("shop group");
    }

    @Test
    public void hardwareRequestWholeWorkflowWithPlannedStartDate() {
        var nowLocalDateTime = LocalDateTime.now();
        var startPlannedDateTimeOneMothLater = nowLocalDateTime.plusMonths(1);
        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(location10)
                                .shopGroupId(shopGroup15)
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        newWorkResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
                )
        );
        assertThat(fullWork).isNotNull();
        assertThat(fullWork.getErrorCode()).isEqualTo(0);
        assertThat(fullWork.getPayload()).isNotNull();

        // update setting general attachments
        var pdfAttachmentResult = assertDoesNotThrow(
                () -> testControllerHelperService.createDummyPDFAttachment(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(pdfAttachmentResult).isNotNull();
        assertThat(pdfAttachmentResult.getErrorCode()).isEqualTo(0);
        var pdfAttachmentId = pdfAttachmentResult.getPayload();

        // update setting general attachments and some other fields
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(getCustomFileIdByName(workTypes.getFirst(), "attachmentsAndFiles"))
                                                        .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult).isNotNull();
        assertThat(updateWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkResult.getPayload()).isTrue();
        // we still are in created state
        assertThat(helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // update adding planned start date
        var updateWorkResult2 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // add plannedStartDateTime
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(getCustomFileIdByName(workTypes.getFirst(), "plannedStartDateTime"))
                                                        .value(ValueDTO.builder().value(helperService.toString(startPlannedDateTimeOneMothLater)).type(ValueTypeDTO.DateTime).build())
                                                        .build(),
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(getCustomFileIdByName(workTypes.getFirst(), "attachmentsAndFiles"))
                                                        .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult2).isNotNull();
        assertThat(updateWorkResult2.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkResult2.getPayload()).isTrue();
        assertThat(helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.PendingApproval)).isTrue();

        // try to put to a ready for work state but should fail by error validations
        var validationErrorOnChangeState = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.ReadyForWork)
                                                .comment("Ready for work from REST API")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(validationErrorOnChangeState).isNotNull();

        // complete the work with these additional custom field: schedulingPriority, accessRequirements, ppsZone, radiationSafetyWorkControlForm, lockAndTag , subsystem,group
        var updateWorkResult3 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .assignedTo(List.of("user2@slac.stanford.edu"))
                                .customFieldValues
                                        (
                                                List.of(
                                                        // set plan start date
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "plannedStartDateTime"))
                                                                .value(ValueDTO.builder().value(helperService.toString(startPlannedDateTimeOneMothLater)).type(ValueTypeDTO.DateTime).build())
                                                                .build(),
                                                        // set attachmentsAndFiles
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "attachmentsAndFiles"))
                                                                .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
                                                        // add group
//                                                        WriteCustomFieldDTO
//                                                                .builder()
//                                                                .id(getCustomFileIdByName(workTypes.getFirst(), "group"))
//                                                                .value(
//                                                                        ValueDTO.builder()
//                                                                                .value(getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "group", 0))
//                                                                                .type(ValueTypeDTO.LOV).build())
//                                                                .build()
                                                )
                                        )
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.ReadyForWork)
                                                .comment("Ready for work from REST API")
                                                .build()
                                )
                                .build()
                )
        );

        // now the workflow should be fallen in ready for work
        assertThat(updateWorkResult3).isNotNull();
        assertThat(updateWorkResult3.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkResult3.getPayload()).isTrue();
        assertThat(helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.ReadyForWork)).isTrue();

        // now advance the day up the planned start date expiration
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
            // jump to the planned start
            mockedStatic.when(LocalDateTime::now).thenReturn(startPlannedDateTimeOneMothLater);

            // Now when LocalDateTime.now() is called, it returns the fixed time
            // simulate the event trigger scheduler
            manageWorkflowUpdateByEventTrigger.processTriggeredEvent();

            await()
                    .atMost(20, SECONDS)
                    .pollInterval(1, SECONDS)
                    .until(
                            () -> {
                                var stateReached = helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.InProgress);
                                return stateReached;
                            }
                    );
        }

    }
}
