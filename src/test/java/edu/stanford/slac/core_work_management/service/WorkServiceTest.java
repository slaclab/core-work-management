package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.*;
import jakarta.validation.ConstraintViolationException;
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
public class WorkServiceTest {
    @Autowired
    WorkService workService;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    LocationService locationService;
    @Autowired
    ShopGroupService shopGroupService;
    private String shopGroupId;
    private String locationId;

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

        locationId =
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO
                                        .builder()
                                        .name("SLAC")
                                        .description("SLAC National Accelerator Laboratory")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();
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
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.ensureActivityType(
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
    public void updateActivityType() {
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.ensureActivityType(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotNull();
        //update activity type
        assertDoesNotThrow(
                () -> workService.updateActivityType(
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 updated")
                                .description("Activity 1 description updated")
                                .customFields(
                                        List.of(
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(true)
                                                        .build(),
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field2")
                                                        .description("custom field2 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        // retrieve and check the full activity type
        var fullUpdatedActivityType = assertDoesNotThrow(
                ()->workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("custom field1");
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("customField1");
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("custom field2");
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("customField2");
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field2 description");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isFalse();

        //update activity type adding a new custom filed and modifying the other
        ActivityTypeDTO finalFullUpdatedActivityType = fullUpdatedActivityType;
        assertDoesNotThrow(
                () -> workService.updateActivityType(
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 re-updated")
                                .description("Activity 1 description re-updated")
                                .customFields(
                                        List.of(
                                                finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .name("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                finalFullUpdatedActivityType.customFields().get(1).toBuilder()
                                                        .name("custom field2 updated")
                                                        .description("custom field2 description updated")
                                                        .valueType(ValueTypeDTO.Number)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build(),
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedActivityType = assertDoesNotThrow(
                ()->workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(3);
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("custom field2 updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("customField2"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field2 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Number);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(2).name()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(2).label()).isEqualTo("customField3");
        assertThat(fullUpdatedActivityType.customFields().get(2).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedActivityType.customFields().get(2).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(2).isMandatory()).isFalse();

        //update activity type removing an attribute
        assertDoesNotThrow(
                () -> workService.updateActivityType(
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 re-updated")
                                .description("Activity 1 description re-updated")
                                .customFields(
                                        List.of(
                                                finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .name("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedActivityType = assertDoesNotThrow(
                ()->workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("customField3");
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isFalse();
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
                                .locationId(locationId)
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
                                .locationId(locationId)
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
    public void createNewActivityOK() {
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
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();
        // create new activity type for work type
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.ensureActivityType(
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
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();

        // fetch activity and check field
        var newlyCreatedActivity = assertDoesNotThrow(
                () -> workService.findActivityById(newActivityId)
        );
        assertThat(newlyCreatedActivity).isNotNull();
        assertThat(newlyCreatedActivity.id()).isNotNull();
        assertThat(newlyCreatedActivity.title()).isEqualTo("Activity 1");
        assertThat(newlyCreatedActivity.description()).isEqualTo("Activity 1 description");
        assertThat(newlyCreatedActivity.activityType().id()).isEqualTo(newActivityTypeId);
        assertThat(newlyCreatedActivity.activityTypeSubtype()).isEqualTo(ActivityTypeSubtypeDTO.Other);
    }

    @Test
    public void createNewActivityWithCustomAttributesOK() {
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
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();
        // create new activity type for work type
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .customFields(
                                        List.of(
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotEmpty();

        var fullActivityType = assertDoesNotThrow(
                () -> workService.findActivityTypeById(newActivityTypeId)
        );

        // create new activity OK
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().get(0).id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newActivityId).isNotEmpty();

        // fetch activity and check field
        var newlyCreatedActivity = assertDoesNotThrow(
                () -> workService.findActivityById(newActivityId)
        );
        assertThat(newlyCreatedActivity).isNotNull();
        assertThat(newlyCreatedActivity.id()).isNotNull();
        assertThat(newlyCreatedActivity.title()).isEqualTo("Activity 1");
        assertThat(newlyCreatedActivity.description()).isEqualTo("Activity 1 description");
        assertThat(newlyCreatedActivity.activityType().id()).isEqualTo(newActivityTypeId);
        assertThat(newlyCreatedActivity.activityTypeSubtype()).isEqualTo(ActivityTypeSubtypeDTO.Other);
        assertThat(newlyCreatedActivity.customFields()).isNotNull();
        assertThat(newlyCreatedActivity.customFields().size()).isEqualTo(1);
        assertThat(newlyCreatedActivity.customFields().get(0).id()).isEqualTo(fullActivityType.customFields().get(0).id());
        assertThat(newlyCreatedActivity.customFields().get(0).name()).isEqualTo("custom field1");
        assertThat(newlyCreatedActivity.customFields().get(0).value().type()).isEqualTo(ValueTypeDTO.String);
        assertThat(newlyCreatedActivity.customFields().get(0).value().value()).isEqualTo("custom field1 value");
    }

    @Test
    public void createNewActivityCheckValidationError() {
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
                                .locationId(locationId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();
        // create new activity type for work type
        String newActivityTypeId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .customFields(
                                        List.of(
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build(),
                                                ActivityTypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field2")
                                                        .description("custom field2 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotEmpty();

        var fullActivityType = assertDoesNotThrow(
                () -> workService.findActivityTypeById(newActivityTypeId)
        );

        // check duplicated id
        var duplicateIdException = assertThrows(
                ControllerLogicException.class,
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().getFirst().id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build(),
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().getFirst().id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        assertThat(duplicateIdException.getErrorCode()).isEqualTo(-1);

        // fail because the id has not been found
        var wrongIdException = assertThrows(
                ControllerLogicException.class,
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id("bad id")
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        assertThat(wrongIdException.getErrorCode()).isEqualTo(-2);

        // fail because the type is not correct
        var badTypeException = assertThrows(
                ControllerLogicException.class,
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().get(1).id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.Date)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        assertThat(badTypeException.getErrorCode()).isEqualTo(-3);

        // fail because the id has not been found
        var mandatoryException = assertThrows(
                ControllerLogicException.class,
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().get(1).id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        assertThat(mandatoryException.getErrorCode()).isEqualTo(-4);
    }
}
