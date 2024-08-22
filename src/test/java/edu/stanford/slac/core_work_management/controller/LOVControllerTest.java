package edu.stanford.slac.core_work_management.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.*;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LOVControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private LOVService lovService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private DomainService domainService;

    private String domainId;
    private List<String> workActivityIds;


    @BeforeAll
    public void setup() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Activity.class);
        mongoTemplate.remove(new Query(), Work.class);

        domainId = domainService.createNew(
                NewDomainDTO.builder()
                        .name("SLAC")
                        .description("SLAC National Accelerator Laboratory")
                        .build()
        );
        // create test work
        workActivityIds = helperService.ensureWorkAndActivitiesTypes(
                domainId,
                NewWorkTypeDTO
                        .builder()
                        .title("Update the documentation")
                        .description("Update the documentation description")
                        .build(),
                ImmutableList.of(
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .customFields(
                                        ImmutableList.of(
                                                WATypeCustomFieldDTO.builder().name("field1").description("field1 description").valueType(ValueTypeDTO.String).isLov(true).build(),
                                                WATypeCustomFieldDTO.builder().name("field2").description("value2 description").valueType(ValueTypeDTO.String).isLov(false).build(),
                                                WATypeCustomFieldDTO.builder().name("field with space").description("field with space description").valueType(ValueTypeDTO.String).isLov(false).build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(workActivityIds).hasSize(2);
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), LOVElement.class);
    }

    @Test
    public void fetchAllLOVFieldForActivity() {
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "field1_group",
                        of(
                                NewLOVElementDTO.builder().value("field1 value1").description("field1 value1 description").build(),
                                NewLOVElementDTO.builder().value("field1 value2").description("field1 value2 description").build()
                        )
                )
        );
        assertDoesNotThrow(
                () -> lovService.associateDomainFieldToGroupName(
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "field1",
                        "field1_group"
                )
        );

        var lovFieldList = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindAllFieldThatAreLOV(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1)
                )
        );
        assertThat(lovFieldList.getErrorCode()).isEqualTo(0);
        assertThat(lovFieldList.getPayload())
                .hasSizeGreaterThan(0);
    }

    @Test
    public void fetchAllLOVFieldForActivityFieldNameWithSpace() {
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "field_with_space_group",
                        of(
                                NewLOVElementDTO.builder().value("field with space value1").description("field with space value1 description").build(),
                                NewLOVElementDTO.builder().value("field with space value2").description("field with space value2 description").build()
                        )
                )
        );

        assertDoesNotThrow(
                () -> lovService.associateDomainFieldToGroupName(
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "fieldWithSpace",
                        "field_with_space_group"
                )
        );

        var fieldThatAreLovList = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindAllFieldThatAreLOV(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1)
                )
        );
        assertThat(fieldThatAreLovList.getErrorCode()).isEqualTo(0);
        assertThat(fieldThatAreLovList.getPayload())
                .hasSize(1);

        var lovFieldList = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindValuesByDomainAndFieldName(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "fieldWithSpace"
                )
        );
        assertThat(lovFieldList.getErrorCode()).isEqualTo(0);
        assertThat(lovFieldList.getPayload())
                .hasSize(2)
                .extracting(LOVElementDTO::value)
                .contains(
                        "field with space value1",
                        "field with space value2"
                );
    }

    @Test
    public void matchFromActivityTypeLOVFieldAndLOVFieldFromRestAPI() {
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "field_with_space_group",
                        of(
                                NewLOVElementDTO.builder().value("field with space value1").description("field with space value1 description").build(),
                                NewLOVElementDTO.builder().value("field with space value2").description("field with space value2 description").build()
                        )
                )
        );

        assertDoesNotThrow(
                () -> lovService.associateDomainFieldToGroupName(
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "fieldWithSpace",
                        "field_with_space_group"
                )
        );

        var fieldThatAreLovList = assertDoesNotThrow(
                () -> testControllerHelperService.lovControllerFindAllFieldThatAreLOV(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1)
                )
        );
        assertThat(fieldThatAreLovList.getErrorCode()).isEqualTo(0);
        assertThat(fieldThatAreLovList.getPayload())
                .hasSize(1)
                .contains("fieldWithSpace");

        var activityType = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindAllActivityTypes(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(activityType.getErrorCode()).isEqualTo(0);
        assertThat(activityType.getPayload())
                .hasSize(1);
        assertThat(activityType.getPayload().get(0).customFields())
                .extracting(WATypeCustomFieldDTO::name)
                .contains("field1","field2","fieldWithSpace");
        assertThat(activityType.getPayload().get(0).customFields())
                .extracting(WATypeCustomFieldDTO::isLov)
                .contains(false, false, true);
    }
}
