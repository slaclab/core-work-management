package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.InvalidLocation;
import edu.stanford.slac.core_work_management.exception.InvalidShopGroup;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.migration.M1004_InitProjectLOV;
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
import java.util.Optional;

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

    private String shopGroupId;
    private String alternateShopGroupId;
    private String locationId;
    private String locationIdOnAlternateDomain;
    private String domainId;
    private String alternateDomainId;
    private List<LOVElementDTO> projectLovValues = null;
    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Test Domain")
                                .description("Test Domain Description")
                                .build()
                )
        );
        assertThat(domainId).isNotEmpty();

        alternateDomainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Alternate Test Domain")
                                .description("Alternate Test Domain Description")
                                .build()
                )
        );
        assertThat(alternateDomainId).isNotEmpty();

        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .domainId(domainId)
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

        alternateShopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .domainId(alternateDomainId)
                                        .name("shop2")
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
        AssertionsForClassTypes.assertThat(alternateShopGroupId).isNotEmpty();

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

        locationIdOnAlternateDomain = assertDoesNotThrow(
                () -> locationService.createNew(
                        NewLocationDTO
                                .builder()
                                .domainId(alternateDomainId)
                                .name("Alternate location")
                                .description("Alternate location description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(locationIdOnAlternateDomain).isNotEmpty();

        // crete lov for 'project' static filed
        M1004_InitProjectLOV m1004_initProjectLOV = new M1004_InitProjectLOV(lovService);
        assertDoesNotThrow(()->m1004_initProjectLOV.changeSet());
        projectLovValues = assertDoesNotThrow(()->lovService.findAllByGroupName("Project"));
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
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field2")
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
                () -> workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1");
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field2");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField2");
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
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                finalFullUpdatedActivityType.customFields().get(1).toBuilder()
                                                        .label("custom field2 updated")
                                                        .description("custom field2 description updated")
                                                        .valueType(ValueTypeDTO.Number)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
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
                () -> workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(3);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field2 updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField2"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field2 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Number);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(2).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(2).name()).isEqualTo("customField3");
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
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
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
                () -> workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField3");
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();
    }

    @Test
    public void updateWorkOK() {
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
                                .domainId(domainId)
                                .title("Work 1")
                                .description("Work 1 description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        assertDoesNotThrow(
                () -> workService.update(
                        newWorkId,
                        UpdateWorkDTO
                                .builder()
                                .title("Update work 1")
                                .description("Update work 1 description")
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
    }

    @Test
    public void updateWorkFailOnInvalidLocationForDomainId() {
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
                                .domainId(domainId)
                                .title("Work 1")
                                .description("Work 1 description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        InvalidLocation invalidLocationForDomainException = assertThrows(
                InvalidLocation.class,
                () -> workService.update(
                        newWorkId,
                        UpdateWorkDTO
                                .builder()
                                .title("Update work 1")
                                .description("Update work 1 description")
                                .locationId(locationIdOnAlternateDomain)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(invalidLocationForDomainException).isNotNull();
        assertThat(invalidLocationForDomainException.getErrorCode()).isEqualTo(-4);
    }

    @Test
    public void updateWorkFailOnInvalidShopGroupForDomainId() {
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
                                .domainId(domainId)
                                .title("Work 1")
                                .description("Work 1 description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        InvalidShopGroup invalidLocationForDomainException = assertThrows(
                InvalidShopGroup.class,
                () -> workService.update(
                        newWorkId,
                        UpdateWorkDTO
                                .builder()
                                .title("Update work 1")
                                .description("Update work 1 description")
                                .locationId(locationId)
                                .shopGroupId(alternateShopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(invalidLocationForDomainException).isNotNull();
        assertThat(invalidLocationForDomainException.getErrorCode()).isEqualTo(-5);
    }

    @Test
    public void createNewWorkFailWithLocationInvalidForDomain() {
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

        // create the work in the alternate domain with the location of the main domain
        InvalidLocation invalidLocationException = assertThrows(
                InvalidLocation.class,
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .domainId(alternateDomainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(invalidLocationException).isNotNull();
        assertThat(invalidLocationException.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void createNewWorkFailWithShopGroupInvalidForDomain() {
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

        // create the work in the alternate domain with the location of the main domain
        InvalidShopGroup invalidLocationException = assertThrows(
                InvalidShopGroup.class,
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(alternateShopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(invalidLocationException).isNotNull();
        assertThat(invalidLocationException.getErrorCode()).isEqualTo(-4);
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        var foundWork = assertDoesNotThrow(
                () -> workService.findWorkById(newWorkId, WorkDetailsOptionDTO.builder().build())
        );
        assertThat(foundWork).isNotNull();
        assertThat(foundWork.id()).isNotNull();
        assertThat(foundWork.domain().id()).isEqualTo(domainId);
    }

    @Test
    public void errorTryToGetWorkWithBadId() {
        var workNotFoundException = assertThrows(
                WorkNotFound.class,
                () -> workService.findWorkById("bad id", WorkDetailsOptionDTO.builder().build())
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
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
        assertThat(newlyCreatedActivity.domain().id()).isEqualTo(domainId);
        assertThat(newlyCreatedActivity.title()).isEqualTo("Activity 1");
        assertThat(newlyCreatedActivity.description()).isEqualTo("Activity 1 description");
        assertThat(newlyCreatedActivity.activityType().id()).isEqualTo(newActivityTypeId);
        assertThat(newlyCreatedActivity.activityTypeSubtype()).isEqualTo(ActivityTypeSubtypeDTO.Other);
    }

    @Test
    public void createNewActivityFailOnWorkCustomFieldInfo() {
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
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
                                .customFields(
                                        List.of(
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(true)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotEmpty();

        var activityType = assertDoesNotThrow(
                () -> workService.findActivityTypeById(newActivityTypeId)
        );
        assertThat(activityType).isNotNull();
        assertThat(activityType.customFields()).isNotNull().hasSize(1);

        // create new activity fails with not type defined
        ConstraintViolationException newActivityFailNotCustomFieldType = assertThrows(
                ConstraintViolationException.class,
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
                                                        .id(activityType.customFields().getFirst().id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
//                                                                        .type(ValueTypeDTO.String)
//                                                                        .value("custom field1 value")
                                                                        .build()
                                                        )
                                                        .build()

                                        )
                                )
                                .build()
                )
        );
        assertThat(newActivityFailNotCustomFieldType).isNotNull();
        assertThat(newActivityFailNotCustomFieldType.getConstraintViolations())
                .isNotNull()
                .hasSize(2);

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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
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
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field1")
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
                                .project(projectLovValues.get(0).id())
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
        assertThat(newlyCreatedActivity.customFields().get(0).name()).isEqualTo("customField1");
        assertThat(newlyCreatedActivity.customFields().get(0).value().type()).isEqualTo(ValueTypeDTO.String);
        assertThat(newlyCreatedActivity.customFields().get(0).value().value()).isEqualTo("custom field1 value");
    }

    @Test
    public void updateActivityWithCustomAttributesOK() {
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
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
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field1")
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
        assertThat(newlyCreatedActivity.id()).isNotNull().isEqualTo(newActivityId);

        // update custom attributes
        assertDoesNotThrow(
                () -> workService.update(
                        newWorkId,
                        newActivityId,
                        UpdateActivityDTO
                                .builder()
                                .title("Activity 1 updated")
                                .description("Activity 1 description updated")
                                .activityTypeId(newActivityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .project(projectLovValues.get(0).id())
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO
                                                        .builder()
                                                        .id(fullActivityType.customFields().get(0).id())
                                                        .value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("custom field1 value updated")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        var updatedActivity = assertDoesNotThrow(
                () -> workService.findActivityById(newActivityId)
        );
        assertThat(updatedActivity).isNotNull();
        assertThat(updatedActivity.id()).isNotNull().isEqualTo(newActivityId);
        assertThat(updatedActivity.title()).isEqualTo("Activity 1 updated");
        assertThat(updatedActivity.description()).isEqualTo("Activity 1 description updated");
        assertThat(updatedActivity.customFields()).isNotNull();
        assertThat(updatedActivity.customFields().size()).isEqualTo(1);
        assertThat(updatedActivity.customFields().get(0).id()).isEqualTo(fullActivityType.customFields().get(0).id());
        assertThat(updatedActivity.customFields().get(0).name()).isEqualTo("customField1");
        assertThat(updatedActivity.customFields().get(0).value().type()).isEqualTo(ValueTypeDTO.String);
        assertThat(updatedActivity.customFields().get(0).value().value()).isEqualTo("custom field1 value updated");
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
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
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .name("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO
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
                                .project(projectLovValues.get(0).id())
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

        assertThat(badTypeException.getErrorCode()).isEqualTo(-5);

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

    @Test
    public void testWorkChanges() {
        // create base work
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
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        var foundWorkWithHistory = assertDoesNotThrow(
                () -> workService.findWorkById(newWorkId, WorkDetailsOptionDTO.builder().changes(Optional.of(true)).build())
        );
        assertThat(foundWorkWithHistory).isNotNull();
        assertThat(foundWorkWithHistory.id()).isEqualTo(newWorkId);
        assertThat(foundWorkWithHistory.changesHistory()).isNotNull().hasSize(1);

        var foundWorkNoHistory = assertDoesNotThrow(
                () -> workService.findWorkById(newWorkId, WorkDetailsOptionDTO.builder().changes(Optional.of(false)).build())
        );
        assertThat(foundWorkNoHistory).isNotNull();
        assertThat(foundWorkNoHistory.id()).isEqualTo(newWorkId);
        assertThat(foundWorkNoHistory.changesHistory()).isNotNull().isEmpty();
    }
}
