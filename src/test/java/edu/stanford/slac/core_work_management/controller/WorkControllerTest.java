/*
 * -----------------------------------------------------------------------------
 * Title      : WorkControllerTest
 * ----------------------------------------------------------------------------
 * File       : WorkControllerTest.java
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

package edu.stanford.slac.core_work_management.controller;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.HelperService;
import edu.stanford.slac.core_work_management.service.LocationService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.WorkService;
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

import java.util.*;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * -----------------------------------------------------------------------------
 * Title      : WorkController
 * ----------------------------------------------------------------------------
 * File       : WorkController.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * Created    : 2/27/24
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 * contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 **/


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private WorkService workService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    private final List<String> testShopGroupIds = new ArrayList<>();
    private final List<String> testLocationIds = new ArrayList<>();
    private final List<String> testWorkTypeIds = new ArrayList<>();
    private final Map<String, List<String>> testActivityTypeIds = new HashMap<>();

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .userEmails(of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                        .build()
                        )
                )
        );
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop2")
                                        .description("shop1 user[1-2]")
                                        .userEmails(of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                                        .build()
                        )
                )
        );

        // create location for test
        testLocationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO.builder()
                                        .name("location1")
                                        .description("location1 description")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .locationShopGroupId(testShopGroupIds.get(0))
                                        .build()
                        )
                )
        );
        testLocationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO.builder()
                                        .name("location2")
                                        .description("location2 description")
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .locationShopGroupId(testShopGroupIds.get(1))
                                        .build()
                        )
                )
        );

        // create work 1
        testWorkTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureWorkType(
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type 1")
                                        .description("Work type 1 description")
                                        .build()
                        )
                )
        );
        // create activity type for work 1
        testActivityTypeIds.put(testWorkTypeIds.get(0), new ArrayList<>());
        testActivityTypeIds.get(testWorkTypeIds.get(0)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(0),
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 1 work type 1")
                                        .description("Activity 1 description")
                                        .build()
                        )
                )
        );
        testActivityTypeIds.get(testWorkTypeIds.get(0)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(0),
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 2 work type 1")
                                        .description("Activity 2 description")
                                        .build()
                        )
                )
        );

        // create work 2
        testWorkTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureWorkType(
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type 2")
                                        .description("Work type 2 description")
                                        .build()
                        )
                )
        );
        // create activity type for work 1
        testActivityTypeIds.put(testWorkTypeIds.get(0), new ArrayList<>());
        testActivityTypeIds.get(testWorkTypeIds.get(0)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(0),
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 1 work type 2")
                                        .description("Activity 1 description")
                                        .build()
                        )
                )
        );
        testActivityTypeIds.get(testWorkTypeIds.get(0)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(0),
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 2 work type 2")
                                        .description("Activity 2 description")
                                        .build()
                        )
                )
        );
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);
    }

    @Test
    public void testCreateNewWork() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        ()->testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
        );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
    }
}
