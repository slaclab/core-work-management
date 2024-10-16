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
public class TecHardwareReportTest {
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
                                .workTypeId(domainTestInfo.getWorkTypeByName("Hardware Report").id())
                                .build()
                )
        );
        assertThat(failForMandatoryField).isNotNull();
        // check that message contains the needed field
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("title");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("description");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("location");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("shopGroup");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("group");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("urgency");
    }

    @Test
    public void wholeWorkflowWithPlannedStartDate() {
        // fetch the report work type
        var reportWorkType = domainTestInfo.getWorkTypeByName("Hardware Report");
        assertThat(reportWorkType).isNotNull();
        // create a new hardware report
        var newHWRequestResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(reportWorkType.id())
                                .title("Report 1")
                                .description("report 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .customFieldValues(
                                        List.of(
                                                // set group
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(reportWorkType, "group"))
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(reportWorkType, "group", 0))
                                                                        .type(ValueTypeDTO.LOV).build()
                                                        )
                                                        .build(),
                                                // set urgency
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(reportWorkType, "urgency"))
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(reportWorkType, "urgency", 0))
                                                                        .type(ValueTypeDTO.LOV).build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newHWRequestResult).isNotNull();
        assertThat(newHWRequestResult.getErrorCode()).isEqualTo(0);
        assertThat(newHWRequestResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // create a hardware request for this report
        LocalDateTime now =  LocalDateTime.now();
        var newHWRequestId = createNewRequestAndSendInReadyToWork(newHWRequestResult.getPayload(), "HW Request 1", now.plusDays(1));

        // now the report should be in scheduled
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Scheduled)).isTrue();

        // simulate the start of the request and wait for the request to be in progress
        processPendingRequestAtDateAndWait(now.plusDays(1), newHWRequestId);

        // check the request should be in progress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // check the report should be in progress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        // simulate the completion of the request
        completeWork(newHWRequestId);

        // the request should have gone in rev
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestId, WorkflowStateDTO.WorkComplete)).isTrue();
        // the report should remain in progress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        // now close the request
        close(newHWRequestId);

        // the request now should be in closed
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestId, WorkflowStateDTO.Closed)).isTrue();
        // the report should goes in review to close
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.ReviewToClose)).isTrue();

        // close the report
        close(newHWRequestResult.getPayload());

        // the report now should be closed
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Closed)).isTrue();
    }

    @Test
    public void wholeWorkflowWithPlannedStartDateAndTwoActivity() {
        // fetch the report work type
        var reportWorkType = domainTestInfo.getWorkTypeByName("Hardware Report");
        assertThat(reportWorkType).isNotNull();
        // create a new hardware report
        var newHWRequestResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(reportWorkType.id())
                                .title("Report 1")
                                .description("report 1 description")
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .customFieldValues(
                                        List.of(
                                                // set group
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(reportWorkType, "group"))
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(reportWorkType, "group", 0))
                                                                        .type(ValueTypeDTO.LOV).build()
                                                        )
                                                        .build(),
                                                // set urgency
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(tecDomainEnvironmentTest.getCustomFileIdByName(reportWorkType, "urgency"))
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(reportWorkType, "urgency", 0))
                                                                        .type(ValueTypeDTO.LOV).build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newHWRequestResult).isNotNull();
        assertThat(newHWRequestResult.getErrorCode()).isEqualTo(0);
        assertThat(newHWRequestResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // create a two hardware request for this report
        LocalDateTime requestOneStartDate =  LocalDateTime.now().plusDays(1);
        LocalDateTime requestTwoStartDate =  LocalDateTime.now().plusDays(2);
        LocalDateTime requestThreeStartDate =  LocalDateTime.now().plusDays(3);

        var newHWOneRequestId = createNewRequestAndSendInReadyToWork(newHWRequestResult.getPayload(), "HW Request 1", requestOneStartDate);
        var newHWTwoRequestId = createNewRequestAndSendInReadyToWork(newHWRequestResult.getPayload(), "HW Request 2", requestTwoStartDate);
        var newHWThreeRequestId = createNewRequestAndSendInReadyToWork(newHWRequestResult.getPayload(), "HW Request 3", requestThreeStartDate);

        // activity one should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWOneRequestId, WorkflowStateDTO.ReadyForWork)).isTrue();
        // activity two should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.ReadyForWork)).isTrue();
        // activity three should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWThreeRequestId, WorkflowStateDTO.ReadyForWork)).isTrue();
        // now the report should be in scheduled
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Scheduled)).isTrue();

        // simulate the start of the request and wait for the request one to be in progress
        processPendingRequestAtDateAndWait(requestOneStartDate, newHWOneRequestId);

        // activity one should be in progress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWOneRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // activity two should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.ReadyForWork)).isTrue();
        // check the report should be in progress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        // simulate the completion and close of the request1
        completeWork(newHWOneRequestId);
        close(newHWOneRequestId);
        // the request should have gone in rev
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWOneRequestId, WorkflowStateDTO.Closed)).isTrue();
        // activity two should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.ReadyForWork)).isTrue();
        // the report should remain in scheduled cause the second activity is not started
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Scheduled)).isTrue();

        // simulate the start of the request two and wait for the request two to be in progress
        processPendingRequestAtDateAndWait(requestTwoStartDate, newHWTwoRequestId);
        // the request should have gone in rev
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWOneRequestId, WorkflowStateDTO.Closed)).isTrue();
        // activity two should be in ready for work
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // the report should remain in scheduled cause the second activity is not started
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        // advance to request three date and schedule the request three
        processPendingRequestAtDateAndWait(requestThreeStartDate, newHWThreeRequestId);
        // activity two should be in InProgress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // activity three should be in InProgress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWThreeRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // the report should remain in scheduled cause the second activity is not started
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        //simulate to close the request three
        completeWork(newHWTwoRequestId);
        close(newHWTwoRequestId);
        // activity two should be in closed
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWTwoRequestId, WorkflowStateDTO.Closed)).isTrue();
        // activity three should be in InProgress
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWThreeRequestId, WorkflowStateDTO.InProgress)).isTrue();
        // the report should remain in scheduled cause the second activity is not started
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.InProgress)).isTrue();

        // now close the last request
        completeWork(newHWThreeRequestId);
        close(newHWThreeRequestId);
        // activity three should be in closed
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWThreeRequestId, WorkflowStateDTO.Closed)).isTrue();
        // the report should be in review to close
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.ReviewToClose)).isTrue();

        // close the report
        close(newHWRequestResult.getPayload());

        // the report now should be closed
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newHWRequestResult.getPayload(), WorkflowStateDTO.Closed)).isTrue();
    }

    /**
     * Create a new hardware request and send it to ready for work
     * @param hwRequestName the name of the hardware request
     * @param plannedStartDateTime the planned start date time
     * @return the new hardware request id
     */
    private String createNewRequestAndSendInReadyToWork(String reportId, String hwRequestName, LocalDateTime plannedStartDateTime) {
        var hardwareRequestWorkType = domainTestInfo.getWorkTypeByName("Hardware Request");
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
                                .workTypeId(hardwareRequestWorkType.id())
                                .parentWorkId(reportId)
                                .title(hwRequestName)
                                .description("%s description".formatted(hwRequestName))
                                .locationId(domainTestInfo.getLocationByName("Location10").id())
                                .shopGroupId(domainTestInfo.getShopGroupByName("Shop15").id())
                                .assignedTo(List.of("user2@slac.stanford.edu"))
                                .customFieldValues
                                        (
                                                List.of(
                                                        // set plan start date
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "plannedStartDateTime"))
                                                                .value(ValueDTO.builder().value(tecDomainEnvironmentTest.toString(plannedStartDateTime)).type(ValueTypeDTO.DateTime).build())
                                                                .build(),
                                                        // add schedulingPriority
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "schedulingPriority"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(hardwareRequestWorkType, "schedulingPriority", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add accessRequirements
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "accessRequirements"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(hardwareRequestWorkType, "accessRequirements", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add ppsZone
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "ppsZone"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(hardwareRequestWorkType, "ppsZone", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build(),
                                                        // add radiationSafetyWorkControlForm
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "radiationSafetyWorkControlForm"))
                                                                .value(ValueDTO.builder().value("false").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add lockAndTag
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "lockAndTag"))
                                                                .value(ValueDTO.builder().value("true").type(ValueTypeDTO.Boolean).build())
                                                                .build(),
                                                        // add subsystem
                                                        WriteCustomFieldDTO
                                                                .builder()
                                                                .id(tecDomainEnvironmentTest.getCustomFileIdByName(hardwareRequestWorkType, "subsystem"))
                                                                .value(
                                                                        ValueDTO.builder()
                                                                                .value(tecDomainEnvironmentTest.getWorkLovValueIdByGroupNameAndIndex(hardwareRequestWorkType, "subsystem", 0))
                                                                                .type(ValueTypeDTO.LOV).build())
                                                                .build()
                                                )
                                        )
                                .build()
                )
        );
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.PendingApproval)).isTrue();

        // update to send in ready to work
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        newWorkResult.getPayload(),
                        UpdateWorkDTO
                                .builder()
                                //send immediately into ready for work
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.ReadyForWork)
                                                .comment("Sent to ready for work")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult).isNotNull();
        assertThat(updateWorkResult.getErrorCode()).isEqualTo(0);

        // return the new request id
        return newWorkResult.getPayload();
    }

    /**
     * Process the pending request at the given date and wait for the work to be in progress
     * @param processingDate the processing date
     * @param requestId the request id
     */
    public void processPendingRequestAtDateAndWait(LocalDateTime processingDate, String requestId) {
        // now advance the day up the planned start date expiration
        when(clock.instant()).thenReturn(processingDate.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Now when LocalDateTime.now() is called, it returns the fixed time
        // simulate the bucket fire event
        manageBucketWorkflowUpdate.processStartAndStop();
        // process the event trigger
        manageWorkflowUpdateByEventTrigger.processTriggeredEvent();
        await()
                .atMost(20, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            var stateReached = tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), requestId, WorkflowStateDTO.InProgress);
                            return stateReached;
                        }
                );

    }

    /**
     * Simulate the request completion
     * @param workId the request or report id
     */
    private void completeWork(String workId) {
        // update to send in ready to work
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        workId,
                        UpdateWorkDTO
                                .builder()
                                //send immediately into ready for work
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.WorkComplete)
                                                .comment("Closed")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult).isNotNull();
        assertThat(updateWorkResult.getErrorCode()).isEqualTo(0);
    }

    /**
     * Simulate the request close operation
     * @param workId the request or report id
     */
    private void close(String workId) {
        // update to send in ready to work
        // update to send in ready to work
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainTestInfo.domain.id(),
                        workId,
                        UpdateWorkDTO
                                .builder()
                                //send immediately into ready for work
                                .workflowStateUpdate(
                                        UpdateWorkflowStateDTO
                                                .builder()
                                                .newState(WorkflowStateDTO.Closed)
                                                .comment("Closed")
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(updateWorkResult).isNotNull();
        assertThat(updateWorkResult.getErrorCode()).isEqualTo(0);
    }

}
