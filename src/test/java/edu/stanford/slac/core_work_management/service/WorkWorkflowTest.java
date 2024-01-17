package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.*;
import jakarta.validation.ConstraintViolationException;
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

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    HelperService helperService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);
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
        assertThat(newActivityId).isNotEmpty();
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
}
