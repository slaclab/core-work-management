package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.InvalidLocation;
import edu.stanford.slac.core_work_management.exception.InvalidShopGroup;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.migration.M1004_InitProjectLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.workflow.DummyChildWorkflow;
import edu.stanford.slac.core_work_management.service.workflow.DummyParentWorkflow;
import org.assertj.core.api.AssertionsForClassTypes;
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

import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceWorkflowArchitectureTestTest {
    @Autowired
    HelperService helperService;
    @Autowired
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
        WorkflowDTO[] workflows = fullDomain.workflows().toArray(new WorkflowDTO[0]);
        newParentWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Parent work type")
                                .description("Parent work type description")
                                .workflowId(workflows[1].id())
                                .build()
                )
        );
        assertThat(newParentWorkTypeId).isNotNull();
        // create child work type
        newChildWorkType = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Child work type")
                                .description("Child work type description")
                                .workflowId(workflows[0].id())
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


        // crete lov for 'project' static filed
        M1004_InitProjectLOV m1004_initProjectLOV = new M1004_InitProjectLOV(lovService);
        assertDoesNotThrow(m1004_initProjectLOV::changeSet);
        projectLovValues = assertDoesNotThrow(() -> lovService.findAllByGroupName("Project"));
    }

    @Test
    public void createNewWork() {
        // create new work
        NewWorkDTO newWorkDTO = NewWorkDTO.builder()
                .title("Test parent work")
                .description("Test parent work description")
                .workTypeId(newParentWorkTypeId)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .project(projectLovValues.get(0).id())
                .build();
        String workId = assertDoesNotThrow(() -> workService.createNew(domainId, newWorkDTO));
        assertThat(workId).isNotEmpty();

        assertThat(helperService.checkStatusOnWork(domainId, workId, WorkflowStateDTO.Created)).isTrue();

        // add child work, send parent to in progress state
        NewWorkDTO newChildWorkDTO = NewWorkDTO.builder()
                .title("Test child work")
                .description("Test child work description")
                .workTypeId(newChildWorkType)
                .locationId(locationId)
                .shopGroupId(shopGroupId)
                .project(projectLovValues.get(0).id())
                .parentWorkId(workId)
                .build();
        String childWorkId = assertDoesNotThrow(() -> workService.createNew(domainId, newChildWorkDTO));
        assertThat(childWorkId).isNotEmpty();

        // check the state of the child work
        assertThat(helperService.checkStatusOnWork(domainId, childWorkId, WorkflowStateDTO.Created)).isTrue();
        // check the state of the parent work
        assertThat(helperService.checkStatusOnWork(domainId, workId, WorkflowStateDTO.InProgress)).isTrue();
    }
}
