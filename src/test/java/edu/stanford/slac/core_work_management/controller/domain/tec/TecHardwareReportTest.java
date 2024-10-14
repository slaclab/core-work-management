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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    }
}
