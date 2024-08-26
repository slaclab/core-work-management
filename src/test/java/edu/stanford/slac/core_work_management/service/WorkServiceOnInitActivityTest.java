package edu.stanford.slac.core_work_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.migration.M1004_InitProjectLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeAll;
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

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceOnInitActivityTest {
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
    @Autowired
    WorkTypeRepository workTypeRepository;
    @Autowired
    ActivityTypeRepository activityTypeRepository;
    @Autowired
    ObjectMapper objectMapper;

    private String domainId;
    private String shopGroupId;
    private String locationId;
    private List<ActivityType> allActivityTypes;
    private List<WorkType> allWorkType;
    private List<LOVElementDTO> projectLovValues;

    @BeforeAll
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);

        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("SLAC")
                                .description("SLAC National Accelerator Laboratory")
                                .build()
                )
        );

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

        locationId =
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO
                                        .builder()
                                        .domainId(domainId
                                        )
                                        .name("SLAC")
                                        .description("SLAC National Accelerator Laboratory")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();
//        M101_InitWorkType initWorkType = new M101_InitWorkType(lovService, workService);
//        assertDoesNotThrow(initWorkType::changeSet);
//        allWorkType = assertDoesNotThrow(
//                () -> workTypeRepository.findAll()
//        );
//        M1002_InitActivityType initActivityType = new M1002_InitActivityType(lovService, workService, activityTypeRepository);
//        assertDoesNotThrow(initActivityType::changeSet);
//        allActivityTypes = assertDoesNotThrow(
//                () -> activityTypeRepository.findAll()
//        );
        M1004_InitProjectLOV m1004_initProjectLOV = new M1004_InitProjectLOV(lovService);
        assertDoesNotThrow(()->m1004_initProjectLOV.changeSet());
        projectLovValues = assertDoesNotThrow(()->lovService.findAllByGroupName("Project"));
    }

    @BeforeEach
    public void clean() {
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);
    }

    @Test
    public void testCreateSoftwareActivity() throws JsonProcessingException {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("Software Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        var testActivityTypeId = allActivityTypes.stream()
                .filter(activityType -> activityType.getTitle().equals("Software Task"))
                .findFirst()
                .map(ActivityType::getId)
                .orElseThrow();
        testCreateActivity(testWorkTypeId, testActivityTypeId);
    }

    @Test
    public void testCreateGeneralActivity() throws JsonProcessingException {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("Software Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        var testActivityTypeId = allActivityTypes.stream()
                .filter(activityType -> activityType.getTitle().equals("General Task"))
                .findFirst()
                .map(ActivityType::getId)
                .orElseThrow();
        testCreateActivity(testWorkTypeId, testActivityTypeId);
    }

    @Test
    public void testCreateHardwareActivity() throws JsonProcessingException {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("Software Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        var testActivityTypeId = allActivityTypes.stream()
                .filter(activityType -> activityType.getTitle().equals("Hardware Task"))
                .findFirst()
                .map(ActivityType::getId)
                .orElseThrow();
        testCreateActivity(testWorkTypeId, testActivityTypeId);
    }

    private void testCreateActivity(String workTypeId, String activityTypeId){

        var fullActivityType = assertDoesNotThrow(()->activityTypeRepository.findById(activityTypeId));

        var writeCustomFieldValue = LOVTestHelper.getInstance(lovService).generateRandomCustomFieldValues(fullActivityType.get().getCustomFields());

        var testWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO.builder()
                                .domainId(domainId)
                                .title("work")
                                .description("work")
                                .workTypeId(workTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );

        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        testWorkId,
                        NewActivityDTO.builder()
                                .title("activity")
                                .description("activity")
                                .activityTypeId(activityTypeId)
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.BugFix)
                                .customFieldValues(writeCustomFieldValue)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );

        var createdActivity = workService.findActivityById(newActivityId);
        assertThat(createdActivity).isNotNull();
        assertThat(createdActivity.customFields()).isNotEmpty();
        assertThat(createdActivity.customFields().size()).isEqualTo(writeCustomFieldValue.size());
        createdActivity.customFields().forEach(
                customFieldDTO -> {
                    var found = createdActivity.customFields().stream()
                            .filter(customField -> customField.id().equals(customFieldDTO.id()))
                            .findFirst();
                    assertThat(found).isNotEmpty();

                    var foundCustomAttribute = fullActivityType.get().getCustomFields().stream()
                            .filter(activityTypeCustomFieldDTO -> activityTypeCustomFieldDTO.getId().equals(customFieldDTO.id()))
                            .findFirst();
                    AssertionsForClassTypes.assertThat(foundCustomAttribute).isNotEmpty();
                    assertThat(found.get().value().type()).isEqualTo(customFieldDTO.value().type());
                    var possibleValue = lovService.findAllByFieldReference(foundCustomAttribute.get().getLovFieldReference());
                    if (!possibleValue.isEmpty()) {
                        assertThat(possibleValue.stream().anyMatch(lovElementDTO -> lovElementDTO.value() .equals(customFieldDTO.value().value()))).isTrue();
                    } else {
                        assertThat(found.get().value().value()).isEqualTo(customFieldDTO.value().value());
                    }
                }
        );
    }

    /**
     * Generate random custom field values
     * @param WATypeCustomFieldDTOS list of custom fields
     * @return custom field values
     */
    private List<WriteCustomFieldDTO> generateRandomCustomFieldValues(List<WATypeCustomFieldDTO> WATypeCustomFieldDTOS) {
        List<WriteCustomFieldDTO> results = new java.util.ArrayList<>();
        WATypeCustomFieldDTOS.forEach(
                activityTypeCustomFieldDTO -> {
                    switch(activityTypeCustomFieldDTO.valueType()) {
                        case String -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(activityTypeCustomFieldDTO.id())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomString(10))
                                                        .type(ValueTypeDTO.String)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Number -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(activityTypeCustomFieldDTO.id())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomNumber())
                                                        .type(ValueTypeDTO.Number)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Date -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(activityTypeCustomFieldDTO.id())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomDate())
                                                        .type(ValueTypeDTO.Date)
                                                        .build()
                                        )
                                        .build()
                        );
                        case DateTime -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(activityTypeCustomFieldDTO.id())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomDateTime())
                                                        .type(ValueTypeDTO.DateTime)
                                                        .build()
                                        )
                                        .build()
                        );
                        case Boolean -> results.add(
                                WriteCustomFieldDTO.builder()
                                        .id(activityTypeCustomFieldDTO.id())
                                        .value(
                                                ValueDTO.builder()
                                                        .value(generateRandomBoolean())
                                                        .type(ValueTypeDTO.Boolean)
                                                        .build()
                                        )
                                        .build()
                        );

                    }
                }
        );
        return results;
    }

    private String generateRandomBoolean() {
        return String.valueOf(java.util.UUID.randomUUID().toString().hashCode() % 2 == 0);
    }

    private String generateRandomDateTime() {
        return java.time.LocalDateTime.now().toString();
    }

    private String generateRandomDate() {
        return java.time.LocalDate.now().toString();
    }

    private String generateRandomNumber() {
        return String.valueOf(java.util.UUID.randomUUID().toString().hashCode());
    }

    private String generateRandomString(int i) {
        return java.util.UUID.randomUUID().toString().substring(0, i);
    }

}
