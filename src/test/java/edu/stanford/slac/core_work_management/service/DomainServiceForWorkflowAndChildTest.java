package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.model.workflow.RecordWorkflow;
import edu.stanford.slac.core_work_management.model.workflow.ReportWorkflow;
import edu.stanford.slac.core_work_management.model.workflow.RequestWorkflow;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "async-ops"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DomainServiceForWorkflowAndChildTest {
    @Autowired
    DomainService domainService;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    WorkRepository workRepository;
    @Autowired
    WorkTypeRepository workTypeRepository;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), WorkType.class);
    }

    @Test
    public void testCreateNewDomainWithWorkflow() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                // add workflow for this domain
                                .workflowImplementations(
                                        Set.of
                                                (
                                                        RequestWorkflow.class.getCanonicalName(),
                                                        RecordWorkflow.class.getCanonicalName(),
                                                        ReportWorkflow.class.getCanonicalName()
                                                )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        var fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );

        assertThat(fullDomain).isNotNull();
        assertThat(fullDomain.workflows()).isNotNull().isNotEmpty().hasSize(3);
        assertThat(fullDomain.workflows().stream().map(WorkflowDTO::name)).containsExactlyInAnyOrder(
                "Request",
                "Record",
                "Report"
        );
    }

    @Test
    public void testCreateNewWorkTypeAssociatingAWorkflow() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                // add workflow for this domain
                                .workflowImplementations(
                                        Set.of
                                                (
                                                        RequestWorkflow.class.getCanonicalName(),
                                                        RecordWorkflow.class.getCanonicalName(),
                                                        ReportWorkflow.class.getCanonicalName()
                                                )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        var fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );
        assertThat(fullDomain).isNotNull();
        assertThat(fullDomain.workflows()).isNotEmpty().hasSize(3);

        // now create a new WorkType associating a workflow
        String workTypeId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO.builder()
                                .title("Test work type")
                                .description("Test work type description")
                                .workflowId(fullDomain.workflows().stream().findFirst().get().id())
                                .build()
                )
        );
        assertThat(workTypeId).isNotNull().isNotEmpty();

        // retrieve the full WorkType to check the correct association
        var fullWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domainId, workTypeId)
        );
        assertThat(fullWorkType).isNotNull();
        assertThat(fullWorkType.workflowId()).isNotNull().isNotEmpty().isEqualTo(fullDomain.workflows().stream().findFirst().get().id());
    }

    @Test
    public void testCreateWorkTypeFailsWithWrongWorkflowId() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                // add workflow for this domain
                                .workflowImplementations(
                                        Set.of
                                                (
                                                        RequestWorkflow.class.getCanonicalName(),
                                                        RecordWorkflow.class.getCanonicalName(),
                                                        ReportWorkflow.class.getCanonicalName()
                                                )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        var fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );
        assertThat(fullDomain).isNotNull();
        assertThat(fullDomain.workflows()).isNotEmpty().hasSize(3);

        // now create a new WorkType associating a workflow
        var workflowNotFoundExc = assertThrows(
                ControllerLogicException.class,
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO.builder()
                                .title("Test work type")
                                .description("Test work type description")
                                .workflowId(UUID.randomUUID().toString())
                                .build()
                )
        );
        assertThat(workflowNotFoundExc).isNotNull();
        assertThat(workflowNotFoundExc.getMessage()).contains("Workflow not found");
    }

    @Test
    public void testCreateWorkTypeFailsWithWrongChildId() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                // add workflow for this domain
                                .workflowImplementations(
                                        Set.of
                                                (
                                                        RequestWorkflow.class.getCanonicalName(),
                                                        RecordWorkflow.class.getCanonicalName(),
                                                        ReportWorkflow.class.getCanonicalName()
                                                )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        // now create a new WorkType associating a workflow
        var childNotFound = assertThrows(
                ControllerLogicException.class,
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO.builder()
                                .title("Test work type")
                                .description("Test work type description")
                                .workflowId(
                                        assertDoesNotThrow(
                                                () -> domainService.findById(domainId)
                                        ).workflows().stream().findFirst().get().id()
                                )
                                .childWorkTypeIds(Set.of(UUID.randomUUID().toString()))
                                .build()
                )
        );
        assertThat(childNotFound).isNotNull();
        assertThat(childNotFound.getMessage()).contains("not found");
    }

    @Test
    public void testCreateWorkTypeFailsWithChild() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                // add workflow for this domain
                                .workflowImplementations(
                                        Set.of
                                                (
                                                        RequestWorkflow.class.getCanonicalName(),
                                                        RecordWorkflow.class.getCanonicalName(),
                                                        ReportWorkflow.class.getCanonicalName()
                                                )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        var fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );
        assertThat(fullDomain).isNotNull();

        // now create a new WorkType that will act as child
        String newChildWorkId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO.builder()
                                .title("Child work type")
                                .description("Child work type description")
                                .workflowId(fullDomain.workflows().stream().findFirst().get().id())
                                .build()
                )
        );
        assertThat(newChildWorkId).isNotNull().isNotEmpty();

        // now create a new WorkType associating a workflow
        String workTypeId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO.builder()
                                .title("Parent work type")
                                .description("Parent work type description")
                                .childWorkTypeIds(Set.of(newChildWorkId))
                                .workflowId(fullDomain.workflows().stream().findFirst().get().id())
                                .build()
                )
        );
        assertThat(workTypeId).isNotNull().isNotEmpty();
    }
}
