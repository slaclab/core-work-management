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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
public class TecHardwareRequestTest {
    @SpyBean
    private Clock clock; // Mock the Clock bean
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private CWMAppProperties cwmAppProperties;
    @Autowired
    private ManageBucketWorkflowUpdate manageBucketWorkflowUpdate;
    @Autowired
    private ManageWorkflowUpdateByEventTrigger manageWorkflowUpdateByEventTrigger;
    @Autowired
    private BaseWorkflowDomainTest tecDomainEnvironmentTest;
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

        // reset the clock to be used to mock the advance of time
        Mockito.reset(clock);
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
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.getWorkTypeByName("Hardware Request").id())
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
    public void wholeWorkflowWithPlannedStartDate() {
        var nowLocalDateTime = LocalDateTime.now();
        var startPlannedDateTimeOneMothLater = nowLocalDateTime.plusMonths(1);
        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
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
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();


        // update adding planned start date
        var updateWorkResult2 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // add plannedStartDateTime
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "plannedStartDateTime"))
                                                        .value(ValueDTO.builder().value(tecDomainEnvironmentTest.toString(startPlannedDateTimeOneMothLater)).type(ValueTypeDTO.DateTime).build())
                                                        .build(),
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.PendingApproval)).isTrue();


        // try to put to a ready for work state but should fail by error validations
        var validationErrorOnChangeState = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
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
                        domainTestInfo.domain.id(),
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
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "plannedStartDateTime"))
                                                                .value(ValueDTO.builder().value(tecDomainEnvironmentTest.toString(startPlannedDateTimeOneMothLater)).type(ValueTypeDTO.DateTime).build())
                                                                .build(),
                                                        // set attachmentsAndFiles
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
                                                                .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.ReadyForWork)).isTrue();


        // now advance the day up the planned start date expiration
        when(clock.instant()).thenReturn(startPlannedDateTimeOneMothLater.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Now when LocalDateTime.now() is called, it returns the fixed time
        // simulate the event trigger scheduler
        manageWorkflowUpdateByEventTrigger.processTriggeredEvent();

        await()
                .atMost(20, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            var stateReached = tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.InProgress);
                            return stateReached;
                        }
                );

        // now user complete the work
        var updateWorkResult4 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.WorkComplete)
                                                .comment("Completed from REST API")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult4).isNotNull();
        assertThat(updateWorkResult4.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkResult4.getPayload()).isTrue();
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.WorkComplete)).isTrue();

        // now the work is completed can be closed
        var closeWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user10@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.Closed)
                                                .comment("Completed from REST API")
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(closeWorkResult).isNotNull();
        assertThat(closeWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(closeWorkResult.getPayload()).isTrue();
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Closed)).isTrue();
    }


    @Test
    public void wholeWorkflowWithBucket() {
        var nowLocalDateTime = LocalDateTime.now();
        // start buket in one month
        var bucketStartDate = nowLocalDateTime.plusMonths(1);
        // end buket in two days
        var buketEndDate = bucketStartDate.plusDays(2);
        // create new slot to simulate the bucket associate to it
        var bucketSlotResult = assertDoesNotThrow(
                () -> testControllerHelperService.maintenanceControllerCreateNewBucket(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user18@slac.stanford.edu"),
                        NewBucketDTO
                                .builder()
                                .description("test")
                                .domainIds(Set.of(domainTestInfo.domain.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO
                                                        .builder()
                                                        .domainId(domainTestInfo.domain.id())
                                                        .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                                        .build()
                                        )
                                )
                                .type(domainTestInfo.lovElementBucketType.get(0).id())
                                .status(domainTestInfo.lovElementBucketStatus.get(0).id())
                                .from(bucketStartDate)
                                .to(buketEndDate)
                                .build()
                )
        );
        assertThat(bucketSlotResult).isNotNull();

        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
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
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // associate work to a bucket
        var bucketAssociationResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerAssociateWorkToBucket(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        bucketSlotResult.getPayload(),
                        Optional.empty()
                )
        );
        assertThat(bucketAssociationResult).isNotNull();

        // complete the work with these additional custom field: schedulingPriority, accessRequirements, ppsZone, radiationSafetyWorkControlForm, lockAndTag , subsystem,group
        var updateWorkResult3 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .assignedTo(List.of("user2@slac.stanford.edu"))
                                .customFieldValues
                                        (
                                                List.of(
                                                        // set attachmentsAndFiles
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
                                                                .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.ReadyForWork)).isTrue();

        // now advance the day up the planned start date expiration
        when(clock.instant()).thenReturn(bucketStartDate.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Now when LocalDateTime.now() is called, it returns the fixed time
        // simulate the event trigger scheduler
        manageBucketWorkflowUpdate.processStartAndStop();

        await()
                .atMost(20, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            var stateReached = tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.InProgress);
                            return stateReached;
                        }
                );

    }

    @Test
    public void wholeWorkflowFailWithPlannedDataAdnBucket() {
        var nowLocalDateTime = LocalDateTime.now();
        // start buket in one month
        var bucketStartDate = nowLocalDateTime.plusMonths(1);
        // end buket in two days
        var buketEndDate = bucketStartDate.plusDays(2);
        // create new slot to simulate the bucket associate to it
        var bucketSlotResult = assertDoesNotThrow(
                () -> testControllerHelperService.maintenanceControllerCreateNewBucket(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user18@slac.stanford.edu"),
                        NewBucketDTO
                                .builder()
                                .description("test")
                                .domainIds(Set.of(domainTestInfo.domain.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO
                                                        .builder()
                                                        .domainId(domainTestInfo.domain.id())
                                                        .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                                        .build()
                                        )
                                )
                                .type(domainTestInfo.lovElementBucketType.get(0).id())
                                .status(domainTestInfo.lovElementBucketStatus.get(0).id())
                                .from(bucketStartDate)
                                .to(buketEndDate)
                                .build()
                )
        );
        assertThat(bucketSlotResult).isNotNull();

        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
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
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
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
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // associate work to a bucket
        var bucketAssociationResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerAssociateWorkToBucket(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        bucketSlotResult.getPayload(),
                        Optional.empty()
                )
        );
        assertThat(bucketAssociationResult).isNotNull();

        // complete the work with these additional custom field: adding planned data too to test incompatibility between bucket and planned data
        var failWithPlannedAndBucket = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
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
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "plannedStartDateTime"))
                                                                .value(ValueDTO.builder().value(tecDomainEnvironmentTest.toString(bucketStartDate)).type(ValueTypeDTO.DateTime).build())
                                                                .build(),
                                                        // set attachmentsAndFiles
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
                                                                .value(ValueDTO.builder().value(pdfAttachmentId).type(ValueTypeDTO.Attachments).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
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
        assertThat(failWithPlannedAndBucket).isNotNull();
    }

    @Test
    public void wholeWorkflowWithPlannedStartDateAuthorizingByAreaManager() {
        var nowLocalDateTime = LocalDateTime.now();
        var startPlannedDateTimeOneMothLater = nowLocalDateTime.plusMonths(1);
        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        // create with normal user
                        Optional.of("user2@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
                )
        );
        assertThat(fullWork).isNotNull();
        assertThat(fullWork.getErrorCode()).isEqualTo(0);
        assertThat(fullWork.getPayload()).isNotNull();

        // complete the work with these additional custom field: schedulingPriority, accessRequirements, ppsZone, radiationSafetyWorkControlForm, lockAndTag , subsystem,group
        var updateWorkResult3 = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // user10 is the area manager for location 10
                        Optional.of("user2@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .assignedTo(List.of("user3@slac.stanford.edu"))
                                .customFieldValues
                                        (
                                                List.of(
                                                        // set plan start date
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "plannedStartDateTime"))
                                                                .value(ValueDTO.builder().value(tecDomainEnvironmentTest.toString(startPlannedDateTimeOneMothLater)).type(ValueTypeDTO.DateTime).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(fullWork.getPayload(), "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
                                                )
                                        )
                                .build()
                )
        );

        // now the workflow should be fallen in ready for work
        assertThat(updateWorkResult3).isNotNull();
        assertThat(updateWorkResult3.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkResult3.getPayload()).isTrue();
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.PendingApproval)).isTrue();

        // normal user should fail to send to ready for work
        var failOnSendToReadyToWork = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().is4xxClientError(),
                        // user10 is the area manager for location 10
                        Optional.of("user2@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.ReadyForWork)
                                                .comment("Completed from REST API")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(failOnSendToReadyToWork).isNotNull();

        var updateWorkForPendingApproval = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // user10 is the area manager for location 10
                        Optional.of("user10@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.ReadyForWork)
                                                .comment("Completed from REST API")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(updateWorkForPendingApproval).isNotNull();
        assertThat(updateWorkForPendingApproval.getErrorCode()).isEqualTo(0);
        assertThat(updateWorkForPendingApproval.getPayload()).isTrue();
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.ReadyForWork)).isTrue();
    }

    @Test
    public void updateAfterBucketAssociation() {
        var nowLocalDateTime = LocalDateTime.now();
        // start buket in one month
        var bucketStartDate = nowLocalDateTime.plusMonths(1);
        // end buket in two days
        var buketEndDate = bucketStartDate.plusDays(2);
        // create new slot to simulate the bucket associate to it
        var bucketSlotResult = assertDoesNotThrow(
                () -> testControllerHelperService.maintenanceControllerCreateNewBucket(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user18@slac.stanford.edu"),
                        NewBucketDTO
                                .builder()
                                .description("test")
                                .domainIds(Set.of(domainTestInfo.domain.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO
                                                        .builder()
                                                        .domainId(domainTestInfo.domain.id())
                                                        .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                                        .build()
                                        )
                                )
                                .type(domainTestInfo.lovElementBucketType.get(0).id())
                                .status(domainTestInfo.lovElementBucketStatus.get(0).id())
                                .from(bucketStartDate)
                                .to(buketEndDate)
                                .build()
                )
        );
        assertThat(bucketSlotResult).isNotNull();

        // create a new hardware request with minimal fields
        var newWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(domainTestInfo.workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // retrieve full work to get the lov value to fill the custom field
        var fullWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
                )
        );
        assertThat(fullWork).isNotNull();
        assertThat(fullWork.getErrorCode()).isEqualTo(0);
        assertThat(fullWork.getPayload()).isNotNull();

        // associate work to a bucket
        var bucketAssociationResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerAssociateWorkToBucket(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        bucketSlotResult.getPayload(),
                        Optional.empty()
                )
        );
        assertThat(bucketAssociationResult).isNotNull();


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
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                .customFieldValues(
                                        List.of(
                                                // set attachmentsAndFiles
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(domainTestInfo.workTypes.getFirst(), "attachmentsAndFiles"))
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
    }
}
