package edu.stanford.slac.core_work_management.service;

import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
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

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkWorkflowTest {
    @Autowired
    WorkService workService;
    @Autowired
    LocationService locationService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    HelperService helperService;
    @Autowired
    MongoTemplate mongoTemplate;
    private String locationId = null;
    private String shopGroupId = null;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);

        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .userEmails(ImmutableSet.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                        .build()
                        )
                );
        assertThat(shopGroupId).isNotEmpty();

        locationId =
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO
                                        .builder()
                                        .name("SLAC")
                                        .description("SLAC National Accelerator Laboratory")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .locationShopGroupId(shopGroupId)
                                        .build()
                        )
                );
        assertThat(locationId).isNotEmpty();
    }

    @Test
    public void creatingNewWorkItIsInNewState() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                emptyList()
        );
        assertThat(listIds).hasSize(1);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.getFirst())
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();
        // work should be in state new
        assertThat(helperService.checkStatusOnWork(newWorkId, WorkStatusDTO.New)).isTrue();
    }

    @Test
    public void createNewActivitySendWorkInScheduledJobOK() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(listIds).hasSize(2);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();

        // create new activity for work plan send it to ScheduledJob state
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(listIds.get(1))
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();

        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityId,
                        of(ActivityStatusDTO.New)
                )
        ).isTrue();
    }

    @Test
    public void createNewActivitySetToCloseSendWorkInReviewToWorkOK() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(listIds).hasSize(2);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();

        // create new activity for work plan send it to ScheduledJob state
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(listIds.get(1))
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();

        // change status to close for the job
        assertDoesNotThrow(
                () -> workService.setActivityStatus(
                        newWorkId,
                        newActivityId,
                        UpdateActivityStatusDTO
                                .builder()
                                .newStatus(ActivityStatusDTO.Completed)
                                .followupDescription("Activity has been completed")
                                .build()
                )
        );
        // work latest status should be review
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
        // activity latest status should be completed
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityId,
                        of(
                                ActivityStatusDTO.Completed,
                                ActivityStatusDTO.New
                        )
                )
        ).isTrue();
    }

    @Test
    public void createNewActivitySetToCloseSendWorkInReviewToWorkWithMoreActivityOK() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(listIds).hasSize(2);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();

        // create new activity for work plan send it to ScheduledJob state
        var newActivityId1 = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(listIds.get(1))
                                .build()
                )
        );
        assertThat(newActivityId1).isNotEmpty();

        var newActivityId2 = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(listIds.get(1))
                                .build()
                )
        );
        assertThat(newActivityId2).isNotEmpty();

        // change status to close for the job
        assertDoesNotThrow(
                () -> workService.setActivityStatus(
                        newWorkId,
                        newActivityId1,
                        UpdateActivityStatusDTO
                                .builder()
                                .newStatus(ActivityStatusDTO.Completed)
                                .followupDescription("Activity has been completed")
                                .build()
                )
        );
        // work latest status should be review
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
        // change status to close for the job
        assertDoesNotThrow(
                () -> workService.setActivityStatus(
                        newWorkId,
                        newActivityId2,
                        UpdateActivityStatusDTO
                                .builder()
                                .newStatus(ActivityStatusDTO.Completed)
                                .followupDescription("Activity has been completed")
                                .build()
                )
        );
        // work latest status should be review
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
        // activity latest status should be completed
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityId1,
                        of(
                                ActivityStatusDTO.Completed,
                                ActivityStatusDTO.New
                        )
                )
        ).isTrue();
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityId2,
                        of(
                                ActivityStatusDTO.Completed,
                                ActivityStatusDTO.New
                        )
                )
        ).isTrue();
    }

    @Test
    public void errorClosingWorkInWrongState() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(listIds).hasSize(2);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );

        var exceptionOnBadClose = assertThrows(
                ControllerLogicException.class,
                () -> workService.reviewWork(
                        newWorkId,
                        ReviewWorkDTO
                                .builder()
                                .followUpDescription("Work has been completed")
                                .build()
                )
        );
        assertThat(exceptionOnBadClose.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void closeWorkWhenInReviewOK() {
        var listIds = helperService.ensureWorkAndActivitiesTypes(
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(listIds).hasSize(2);
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(listIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();

        // create new activity for work plan send it to ScheduledJob state
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(listIds.get(1))
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();

        // change status to close for the job
        assertDoesNotThrow(
                () -> workService.setActivityStatus(
                        newWorkId,
                        newActivityId,
                        UpdateActivityStatusDTO
                                .builder()
                                .newStatus(ActivityStatusDTO.Completed)
                                .followupDescription("Activity has been completed")
                                .build()
                )
        );
        // work latest status should be review
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
        // activity latest status should be completed
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityId,
                        of(
                                ActivityStatusDTO.Completed,
                                ActivityStatusDTO.New
                        )
                )
        ).isTrue();

        // close work
        assertDoesNotThrow(
                () -> workService.reviewWork(
                        newWorkId,
                        ReviewWorkDTO
                                .builder()
                                .followUpDescription("Work has been completed")
                                .build()
                )
        );
        // check all status
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkId,
                        of(
                                WorkStatusDTO.Closed,
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
    }
}
