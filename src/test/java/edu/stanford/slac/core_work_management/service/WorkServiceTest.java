package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceTest {
    @Autowired
    WorkService workService;
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
    public void createNewWorkType() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );

        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);
    }

    @Test
    public void createNewActivityType() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Work type 1")
                                .description("Work type 1 description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.ensureActivityType(
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotNull();
    }

    @Test
    public void findAllActivityForAWorkId() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        String newActivityTypeId1 = assertDoesNotThrow(
                () -> workService.ensureActivityType(
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId1).isNotNull();
        String newActivityTypeId2 = assertDoesNotThrow(
                () -> workService.ensureActivityType(
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 2")
                                .description("Activity 2 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId2).isNotNull();

        var allActivityFound = assertDoesNotThrow(
                () -> workService.findAllActivityTypesByWorkId(newWorkTypeId)
        );
        assertThat(allActivityFound)
                .isNotNull()
                .hasSize(2)
                .extracting(ActivityTypeDTO::id)
                .contains(newActivityTypeId1, newActivityTypeId2);
    }

    @Test
    public void errorCreatingNewActivityWithBadWorkId() {
        WorkNotFound noWorkFoundException = assertThrows(
                WorkNotFound.class,
                () -> workService.ensureActivityType(
                        "bad id",
                        NewActivityTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(noWorkFoundException).isNotNull();
        assertThat(noWorkFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createNewWork() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();
    }

    @Test
    public void createNewWorkAndGetIt() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        var foundWork = assertDoesNotThrow(
                () -> workService.findWorkById(newWorkId)
        );
        assertThat(foundWork).isNotNull();
        assertThat(foundWork.id()).isNotNull();
    }

    @Test
    public void errorTryToGetWorkWithBadId() {
        var workNotFoundException = assertThrows(
                WorkNotFound.class,
                () -> workService.findWorkById("bad id")
        );
        assertThat(workNotFoundException).isNotNull();
        assertThat(workNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createNewActivityOK(){
        //create work type
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();
        // create new activity type for work type
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.ensureActivityType(
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotEmpty();

        // create new activity fail with wrong DTO
        var errorCreatingActivityWithEmptyDTO = assertThrows(
                ConstraintViolationException.class,
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .build()
                )
        );
        assertThat(errorCreatingActivityWithEmptyDTO).isNotNull();

        // create new activity OK
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();
    }
}
