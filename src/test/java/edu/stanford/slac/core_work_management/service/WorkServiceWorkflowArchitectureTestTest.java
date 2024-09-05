package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.WorkCannotHaveChildren;
import edu.stanford.slac.core_work_management.exception.WorkflowNotManuallyUpdatable;
import edu.stanford.slac.core_work_management.model.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceWorkflowArchitectureTestTest {
    @Autowired
    HelperService helperService;
    @SpyBean
    DomainService domainService;
    @Autowired
    WorkService workService;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    LocationService locationService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    LOVService lovService;

    private DomainDTO fullDomain;
    private String shopGroupId;
    private String locationId;
    private String newParentWorkTypeId;
    private String newChildWorkType;
    private String domainId;
    private List<LOVElementDTO> projectLovValues = null;

    @BeforeEach
    public void cleanCollection() {
        reset(domainService);
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        // create domain
        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Test Domain")
                                .description("Test Domain Description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow",
                                                "DummyChildWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotEmpty();

        // fetch full domain
        fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );


        // create parent work type
        // //find parent test workflow
        var parentWorkflow = fullDomain.workflows().stream().filter(workflowDTO -> workflowDTO.implementation().equals("DummyParentWorkflow")).findFirst();
        assertThat(parentWorkflow).isPresent();
        newParentWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Parent work type")
                                .description("Parent work type description")
                                .workflowId(parentWorkflow.get().id())
                                .build()
                )
        );
        assertThat(newParentWorkTypeId).isNotNull();
        // create child work type
        // //find child test workflow
        var childWorkflow = fullDomain.workflows().stream().filter(workflowDTO -> workflowDTO.implementation().equals("DummyChildWorkflow")).findFirst();
        assertThat(childWorkflow).isPresent();
        newChildWorkType = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Child work type")
                                .description("Child work type description")
                                .workflowId(childWorkflow.get().id())
                                .build()
                )
        );
        assertThat(newChildWorkType).isNotNull();

        // crate shop groups
        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainId,
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .users(
                                                of(
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

        locationId = assertDoesNotThrow(
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
    }

    /**
     * Create a new work and check if the state is created
     * then add a child work and check if the parent work is in progress
     */
    @Test
    public void createNewWorkWithChildAccordingToDummyWorkflow() {
        // create new work
        NewWorkDTO newWorkDTO = NewWorkDTO.builder()
                .title("Test parent work")
                .description("Test parent work description")
                .workTypeId(newParentWorkTypeId)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .build();
        String newParentWorkId = assertDoesNotThrow(() -> workService.createNew(domainId, newWorkDTO));
        assertThat(newParentWorkId).isNotEmpty();

        assertThat(helperService.checkStatusOnWork(domainId, newParentWorkId, WorkflowStateDTO.Created)).isTrue();

        // add child work, send parent to in progress state
        NewWorkDTO newChildWorkDTO = NewWorkDTO.builder()
                .title("Test child work")
                .description("Test child work description")
                .workTypeId(newChildWorkType)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .parentWorkId(newParentWorkId)
                .build();
        String newChildWorkId = assertDoesNotThrow(() -> workService.createNew(domainId, newChildWorkDTO));
        assertThat(newChildWorkId).isNotEmpty();

        // check the state of the child work
        assertThat(helperService.checkStatusOnWork(domainId, newChildWorkId, WorkflowStateDTO.Created)).isTrue();

        // check the state of the parent work
        assertThat(helperService.checkStatusOnWork(domainId, newParentWorkId, WorkflowStateDTO.InProgress)).isTrue();

        // now try to close the parent work
        var stateNotPermitted = assertThrows(
                WorkflowNotManuallyUpdatable.class,
                () -> workService.update(
                        domainId,
                        newParentWorkId,
                        UpdateWorkDTO.builder()
                                .workflowStateUpdate(UpdateWorkflowStateDTO.builder().newState(WorkflowStateDTO.Closed).build())
                                .build()
                )
        );
        assertThat(stateNotPermitted).isNotNull();

        // now close the child work
        assertDoesNotThrow(() -> workService.update(
                domainId,
                newChildWorkId,
                UpdateWorkDTO.builder()
                        .workflowStateUpdate(UpdateWorkflowStateDTO.builder().newState(WorkflowStateDTO.Closed).build())
                        .build()
        ));

        // check the state of the child work
        assertThat(helperService.checkStatusOnWork(domainId, newChildWorkId, WorkflowStateDTO.Closed)).isTrue();
        // parent work should be gone in review to close state
        assertThat(helperService.checkStatusOnWork(domainId, newParentWorkId, WorkflowStateDTO.ReviewToClose)).isTrue();

        // now try to add another children should give exception
        var parentWorkCannotHaveChildren = assertThrows(
                WorkCannotHaveChildren.class,
                () -> workService.createNew(domainId, newChildWorkDTO)
        );
        assertThat(parentWorkCannotHaveChildren).isNotNull();

        // now close the parent work
        assertDoesNotThrow(() -> workService.update(
                domainId,
                newParentWorkId,
                UpdateWorkDTO.builder()
                        .workflowStateUpdate(UpdateWorkflowStateDTO
                                .builder()
                                .newState(WorkflowStateDTO.Closed)
                                .comment("closing parent work")
                                .build())
                        .build()
        ));

        // check the state of the parent work
        assertThat(helperService.checkStatusOnWork(domainId, newParentWorkId, WorkflowStateDTO.Closed, "closing parent work")).isTrue();
        // check all full history of both works
        assertThat(helperService.checkStatusAndHistoryOnWork(
                domainId,
                newChildWorkId,
                List.of(
                        WorkflowStateDTO.Closed,
                        WorkflowStateDTO.Created
                ))).isTrue();
        assertThat(helperService.checkStatusAndHistoryOnWork(
                domainId,
                newParentWorkId,
                List.of(
                        WorkflowStateDTO.Closed,
                        WorkflowStateDTO.ReviewToClose,
                        WorkflowStateDTO.InProgress,
                        WorkflowStateDTO.Created
                ))).isTrue();
    }

    @Test
    public void testWorkflowOnFailedTransaction() {
        // create new work
        NewWorkDTO newWorkDTO = NewWorkDTO.builder()
                .title("Test parent work")
                .description("Test parent work description")
                .workTypeId(newParentWorkTypeId)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .build();
        String workId = assertDoesNotThrow(() -> workService.createNew(domainId, newWorkDTO));
        assertThat(workId).isNotEmpty();

        assertThat(helperService.checkStatusOnWork(domainId, workId, WorkflowStateDTO.Created)).isTrue();

        // Temporarily mock the domainService to fails after the
        // parent workflow has been updated and saved
        doThrow(ControllerLogicException.builder().build()).when(domainService).updateDomainStatistics(domainId);

        // add child work, send parent to in progress state
        NewWorkDTO newChildWorkDTO = NewWorkDTO.builder()
                .title("Test child work")
                .description("Test child work description")
                .workTypeId(newChildWorkType)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .parentWorkId(workId)
                .build();
        ControllerLogicException exceptionOnUpdateStatistic = assertThrows(
                ControllerLogicException.class,
                () -> workService.createNew(domainId, newChildWorkDTO));
        assertThat(exceptionOnUpdateStatistic).isNotNull();

        // check the state of the parent work
        assertThat(helperService.checkStatusOnWork(domainId, workId, WorkflowStateDTO.Created)).isTrue();
    }
}
