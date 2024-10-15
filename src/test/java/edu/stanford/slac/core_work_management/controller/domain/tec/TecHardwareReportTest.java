package edu.stanford.slac.core_work_management.controller.domain.tec;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
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
        var newWorkResult = assertDoesNotThrow(
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
        assertThat(newWorkResult).isNotNull();
        assertThat(newWorkResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkResult.getPayload()).isNotNull();
        // we are in created state
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();

        // create a new child a new child for it
        LocalDateTime now =  LocalDateTime.now();
        var newHWRequestId = createNewRequestAndSendInReadyToWork(newWorkResult.getPayload(), "HW Request 1", now.plusDays(1));

        // now the report should be in scheduled
        assertThat(tecDomainEnvironmentTest.checkWorkflowStatus(domainTestInfo.domain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Scheduled)).isTrue();
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
        // simulate the event trigger scheduler
        manageBucketWorkflowUpdate.processStartAndStop();

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
     * @param requestId the request id
     */
    private void completeRequest(String requestId) {
        // update to send in ready to work
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanfod.edu"),
                        domainTestInfo.domain.id(),
                        requestId,
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
     * @param requestId the request id
     */
    private void closeRequest(String requestId) {
        // update to send in ready to work
        // update to send in ready to work
        var updateWorkResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanfod.edu"),
                        domainTestInfo.domain.id(),
                        requestId,
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
