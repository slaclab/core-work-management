package edu.stanford.slac.core_work_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.migration.InitActivityType;
import edu.stanford.slac.core_work_management.migration.InitWorkType;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
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
import java.util.UUID;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceOnInitWorkTest {
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
    private String shopGroupId;
    private String locationId;
    private List<ActivityType> allActivityTypes;
    private List<WorkType> allWorkType;
    @Autowired
    ObjectMapper objectMapper;
    @BeforeAll
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);

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
        InitWorkType initWorkType = new InitWorkType(lovService, workService);
        assertDoesNotThrow(initWorkType::changeSet);
        allWorkType = assertDoesNotThrow(
                () -> workTypeRepository.findAll()
        );
        InitActivityType initActivityType = new InitActivityType(lovService, workService, activityTypeRepository);
        assertDoesNotThrow(initActivityType::changeSet);
        allActivityTypes = assertDoesNotThrow(
                () -> activityTypeRepository.findAll()
        );
    }

    @BeforeEach
    public void clean() {
        mongoTemplate.remove(new Query(), Work.class);
    }

    @Test
    public void testCreateSoftwareIssues() {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("Software Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        testCreateWork(testWorkTypeId);
    }

    @Test
    public void testCreateHardwareIssues() {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("Hardware Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        testCreateWork(testWorkTypeId);
    }

    @Test
    public void testCreateGeneralIssues() {
        var testWorkTypeId = allWorkType.stream()
                .filter(workType -> workType.getTitle().equals("General Issues"))
                .findFirst()
                .map(WorkType::getId)
                .orElseThrow();
        testCreateWork(testWorkTypeId);
    }

    private void testCreateWork(String workTypeId) {
        var fullWorkType = assertDoesNotThrow(()->workService.findWorkTypeById(workTypeId));

        var witeCustomFieldValue = generateRandomCustomFieldValues(fullWorkType.customFields());
        var testWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO.builder()
                                .title(UUID.randomUUID().toString())
                                .description(UUID.randomUUID().toString())
                                .workTypeId(workTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .customFieldValues(witeCustomFieldValue)
                                .build()
                )
        );

        var createdWork = workService.findWorkById(testWorkId);
        assertThat(createdWork).isNotNull();
        assertThat(createdWork.customFields()).isNotEmpty();
        assertThat(createdWork.customFields().size()).isEqualTo(witeCustomFieldValue.size());
        witeCustomFieldValue.forEach(
                writeCustomFieldDTO -> {
                    var found = createdWork.customFields().stream()
                            .filter(customField -> customField.id().equals(writeCustomFieldDTO.id()))
                            .findFirst();
                    assertThat(found).isNotEmpty();
                    assertThat(found.get().value().value()).isEqualTo(writeCustomFieldDTO.value().value());
                    assertThat(found.get().value().type()).isEqualTo(writeCustomFieldDTO.value().type());
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
