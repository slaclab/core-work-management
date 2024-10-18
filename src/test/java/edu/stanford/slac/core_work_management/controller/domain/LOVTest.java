package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
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
public class LOVTest {
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

    @Test
    public void testHWReportLOV() {
        var foundLOV = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindAllFieldThatAreLOV(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Work,
                        domainTestInfo.domain.id(),
                        domainTestInfo.getWorkTypeByName("Hardware Report").id()
                )
        );
        assertThat(foundLOV).isNotNull();
        assertThat(foundLOV.getErrorCode()).isEqualTo(0);
        assertThat(foundLOV.getErrorMessage()).isNull();
        assertThat(foundLOV.getPayload()).isNotNull().isNotEmpty().hasSize(8);
        assertThat(foundLOV.getPayload()).contains("unit", "urgency", "micro", "issuePriority", "project", "subsystem", "facility", "primary");
    }

    @Test
    public void testHWRequestLOV() {
        var foundLOV = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindAllFieldThatAreLOV(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Work,
                        domainTestInfo.domain.id(),
                        domainTestInfo.getWorkTypeByName("Hardware Request").id()
                )
        );
        assertThat(foundLOV).isNotNull();
        assertThat(foundLOV.getErrorCode()).isEqualTo(0);
        assertThat(foundLOV.getErrorMessage()).isNull();
        assertThat(foundLOV.getPayload()).isNotNull().isNotEmpty().hasSize(10);
        assertThat(foundLOV.getPayload()).contains("schedulingPriority","unit","micro","microOther","ppsZone","accessRequirements","subsystem","project","building","primary");
    }
}
