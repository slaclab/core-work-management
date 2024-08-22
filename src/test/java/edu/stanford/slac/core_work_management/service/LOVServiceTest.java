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
import edu.stanford.slac.core_work_management.migration.M1004_InitProjectLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.LOVElementRepository;
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
    private MongoTemplate mongoTemplate;
    @Autowired
    private LOVService lovService;
    @Autowired
    private LOVElementRepository lovElementRepository;
    @Autowired
    private HelperService helperService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private WorkService workService;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private LocationService locationService;

    private String domainId;
    private List<String> workActivityIds;
    private String shopGroupId;
    private String locationId;

    @BeforeAll
    public void setup() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), Activity.class);
        mongoTemplate.remove(new Query(), Work.class);
        // create domain
        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Domain 1")
                                .description("Domain 1 description")
                                .build()
                )
        );
        assertThat(domainId).isNotEmpty();
        // create test work
        workActivityIds = helperService.ensureWorkAndActivitiesTypes(
                domainId,
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
                                                WATypeCustomFieldDTO.builder().name("field1").description("field1 description").valueType(ValueTypeDTO.String).isLov(true).build(),
                                                WATypeCustomFieldDTO.builder().name("field2").description("value2 description").valueType(ValueTypeDTO.String).isLov(false).build()
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
                                        .domainId(domainId)
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
                                        .domainId(domainId)
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
    public void testCreateWithOnlyGroupName() {
        // add lov for static field
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "group-1",
                        of(
                                NewLOVElementDTO.builder().value("group-1 value1").description("group-1 value1 description").build(),
                                NewLOVElementDTO.builder().value("group-1 value2").description("group-1 value2 description").build()
                        )
                )
        );
        var listOfAllLOVGroup1 = assertDoesNotThrow(
                () -> lovService.findAllByGroupName("group-1")
        );
        assertThat(listOfAllLOVGroup1).hasSize(2);
        assertThat(listOfAllLOVGroup1).extracting(LOVElementDTO::value).contains("group-1 value1", "group-1 value2");

        lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "group-2",
                        of(
                                NewLOVElementDTO.builder().value("group-2 value1").description("group-2 value1 description").build(),
                                NewLOVElementDTO.builder().value("group-2 value2").description("group-2 value2 description").build()
                        )
                )
        );
        var listOfAllLOVGroup2 = assertDoesNotThrow(
                () -> lovService.findAllByGroupName("group-2")
        );
        assertThat(listOfAllLOVGroup2).hasSize(2);
        assertThat(listOfAllLOVGroup2).extracting(LOVElementDTO::value).contains("group-2 value1", "group-2 value2");
    }

    @Test
    public void testAddAndRemoveFieldReferenceToGroupName() {
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "group-1",
                        of(
                                NewLOVElementDTO.builder().value("group-1 value1").description("group-1 value1 description").build(),
                                NewLOVElementDTO.builder().value("group-1 value2").description("group-1 value2 description").build()
                        )
                )
        );
        assertDoesNotThrow(
                () -> lovService.addFieldReferenceToGroupName(
                        "group-1",
                        of("field1", "field2")
                )
        );
        // check if the field reference has been added
        var elementList = lovElementRepository.findByGroupNameIs("group-1");
        elementList.forEach(
                element -> {
                    assertThat(element.getFieldReference()).contains("field1", "field2");
                }
        );

        assertDoesNotThrow(
                () -> lovService.removeFieldReferenceFromGroupName(
                        "group-1",
                        of("field2")
                )
        );
        elementList.forEach(
                element -> {
                    assertThat(element.getFieldReference()).contains("field1");
                }
        );
    }

    @Test
    public void createNewLOVElementForDomainAndDynamicField() {
        var lovIds = assertDoesNotThrow(
                () -> lovService.createNew(
                        "field1_group",
                        of(
                                NewLOVElementDTO.builder().value("field1 value1").description("field1 value1 description").build(),
                                NewLOVElementDTO.builder().value("field1 value2").description("field1 value2 description").build()
                        )
                )
        );
        // add lov for dynamic field
        assertDoesNotThrow(
                () -> lovService.associateDomainFieldToGroupName(
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "field1",
                        "field1_group"
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
                () -> lovService.associateDomainFieldToGroupName(
                        LOVDomainTypeDTO.Activity,
                        workActivityIds.get(1),
                        "wrong field",
                        "field1_group"
                )
        );
        assertThat(fieldNotFound.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void validateValueOnCreatedLOV() {
        // crete lov for 'project' static filed
        M1004_InitProjectLOV m1004_initProjectLOV = new M1004_InitProjectLOV(lovService);
        assertDoesNotThrow(()->m1004_initProjectLOV.changeSet());
        var projectLovValues = assertDoesNotThrow(()->lovService.findAllByGroupName("Project"));
        // add lov for dynamic field
        assertDoesNotThrow(
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
        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        NewWorkDTO
                                .builder()
                                .domainId(domainId)
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(workActivityIds.get(0))
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .project(projectLovValues.get(0).id())
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newWorkId).isNotEmpty();

        var listOfAllLOVField1 = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Activity, workActivityIds.get(1), "field1")
        );

        // find the full activity type
        var fullActivityType = domainService.findActivityTypeById(domainId, workActivityIds.get(0), workActivityIds.get(1));

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
        assertThat(fullActivity.customFields()).hasSize(1);
        assertThat(fullActivity.customFields().get(0).value().value()).isEqualTo(listOfAllLOVField1.getFirst().value());
    }
}
