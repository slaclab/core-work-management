/*
 * -----------------------------------------------------------------------------
 * Title      : LOVServiceTest
 * ----------------------------------------------------------------------------
 * File       : LOVServiceTest.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

package edu.stanford.slac.core_work_management.service;

import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.LOVFieldReferenceNotFound;
import edu.stanford.slac.core_work_management.model.*;
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

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LOVServiceTest {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    LOVService lovService;
    @Autowired
    HelperService helperService;
    @Autowired
    WorkService workService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    LocationService locationService;
    private List<String> workActivityIds;
    private String shopGroupId;
    private String locationId;

    @BeforeAll
    public void setup() {
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Activity.class);
        mongoTemplate.remove(new Query(), Work.class);
        // create test work
        workActivityIds = helperService.ensureWorkAndActivitiesTypes(
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
                                .customFields(
                                        of(
                                                ActivityTypeCustomFieldDTO.builder().name("field1").description("field1 description").valueType(ValueTypeDTO.String).isLov(true).build(),
                                                ActivityTypeCustomFieldDTO.builder().name("field2").description("value2 description").valueType(ValueTypeDTO.String).isLov(false).build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(workActivityIds).hasSize(2);

        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .users(
                                                ImmutableSet.of(
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

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), LOVElement.class);
    }

    @Test
    public void createNewLOVElementForDomainAndStaticField() {
        // add lov for static field
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        LOVDomainTypeDTO.Activity,
                        "schedulingProperty",
                        of(
                                NewLOVElementDTO.builder().value("schedulingProperty value1").description("schedulingProperty value1 description").build(),
                                NewLOVElementDTO.builder().value("schedulingProperty value2").description("schedulingProperty value2 description").build()
                        )
                )
        );
        var listOfAllLOV = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Activity, workActivityIds.get(1), "schedulingProperty")
        );
        assertThat(listOfAllLOV).hasSize(2);
        assertThat(listOfAllLOV).extracting(LOVElementDTO::value).contains("schedulingProperty value1", "schedulingProperty value2");
    }

    @Test
    public void createNewLOVElementForDomainAndDynamicField() {
        // add lov for dynamic field
        assertDoesNotThrow(
                () -> lovService.createNew(
                        LOVDomainTypeDTO.Activity,
                        "field1",
                        of(
                                NewLOVElementDTO.builder().value("field1 value1").description("field1 value1 description").build(),
                                NewLOVElementDTO.builder().value("field1 value2").description("field1 value2 description").build()
                        )
                )
        );
        var listOfAllLOV = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Activity, workActivityIds.get(1), "field1")
        );
        assertThat(listOfAllLOV).hasSize(2);
        assertThat(listOfAllLOV).extracting(LOVElementDTO::value).contains("field1 value1", "field1 value2");
    }

    @Test
    public void createNewLOVElementForDomainFailOnWrongFieldName() {
        // add lov for dynamic field
        var fieldNotFound = assertThrows(
                LOVFieldReferenceNotFound.class,
                () -> lovService.createNew(
                        LOVDomainTypeDTO.Activity,
                        "wrong field",
                        of(
                                NewLOVElementDTO.builder().value("field1 value1").description("field1 value1 description").build(),
                                NewLOVElementDTO.builder().value("field1 value2").description("field1 value2 description").build()
                        )
                )
        );
       assertThat(fieldNotFound.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void validateValueOnCreatedLOV() {
        assertDoesNotThrow(
                () -> lovService.createNew(
                        LOVDomainTypeDTO.Activity,
                        "schedulingProperty",
                        of(
                                NewLOVElementDTO.builder().value("schedulingProperty value1").description("value1 description").build(),
                                NewLOVElementDTO.builder().value("schedulingProperty value2").description("value2 description").build()
                        )
                )
        );

        // add lov for dynamic field
        assertDoesNotThrow(
                () -> lovService.createNew(
                        LOVDomainTypeDTO.Activity,
                        "field1",
                        of(
                                NewLOVElementDTO.builder().value("field1 value1").description("field1 value1 description").build(),
                                NewLOVElementDTO.builder().value("field1 value2").description("field1 value2 description").build()
                        )
                )
        );

        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(workActivityIds.get(0))
                                .locationId(locationId)
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newWorkId).isNotEmpty();

        var listOfAllLOVSchedulingProperty = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Activity, workActivityIds.get(1), "schedulingProperty")
        );

        var listOfAllLOVField1 = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Activity, workActivityIds.get(1), "field1")
        );

        // find the full activity type
        var fullActivityType = workService.findActivityTypeById(workActivityIds.get(1));

        // create new activity for work plan send it to ScheduledJob state
        var newActivityId = assertDoesNotThrow(
                () -> workService.createNew(
                        newWorkId,
                        NewActivityDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .activityTypeId(workActivityIds.get(1))
                                .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                .schedulingProperty(listOfAllLOVSchedulingProperty.getFirst().id())
                                .customFieldValues(
                                        of(
                                                WriteCustomFieldDTO.builder()
                                                        .id(
                                                                fullActivityType.customFields().get(0).id()
                                                        ).value(
                                                                ValueDTO
                                                                        .builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value(listOfAllLOVField1.getFirst().id())
                                                                        .build()
                                                        ).build()
                                        )
                                )
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newActivityId).isNotEmpty();

        var fullActivity = assertDoesNotThrow(
                () -> workService.findActivityById(
                        newActivityId
                )
        );
        assertThat(fullActivity.schedulingProperty()).isNotNull();
        assertThat(fullActivity.schedulingProperty().value()).isEqualTo(listOfAllLOVSchedulingProperty.getFirst().value());
        assertThat(fullActivity.customFields()).hasSize(1);
        assertThat(fullActivity.customFields().get(0).value().value()).isEqualTo(listOfAllLOVField1.getFirst().value());
    }
}
