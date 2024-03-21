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

import com.google.common.collect.ImmutableList;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
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
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_FAKE_USER_TEMPLATE;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    AppProperties appProperties;
    @Autowired
    AuthService authService;
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
    private final List<String> testActivityTypeIds = new ArrayList<>();

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), ActivityType.class);
        // create location for test
        testLocationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO.builder()
                                        .name("location1")
                                        .description("location1 description")
                                        .locationManagerUserId("user1@slac.stanford.edu")
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
                                        .locationManagerUserId("user2@slac.stanford.edu")
                                        .build()
                        )
                )
        );
        testLocationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                NewLocationDTO.builder()
                                        .name("location3")
                                        .description("location3 description")
                                        .locationManagerUserId("user2@slac.stanford.edu")
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
        testActivityTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 1")
                                        .description("Activity 1 description")
                                        .build()
                        )
                )
        );
        testActivityTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 2")
                                        .description("Activity 2 description")
                                        .build()
                        )
                )
        );


        // create activity type for work 2
        testActivityTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 3")
                                        .description("Activity 3 description")
                                        .build()
                        )
                )
        );
        testActivityTypeIds.add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 4")
                                        .description("Activity 4 description")
                                        .build()
                        )
                )
        );
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Activity.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        // shop group creation need to be moved here because the manage the authorization for the leader
        testShopGroupIds.clear();
        testShopGroupIds.add(
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
                                                                .isLeader(true)
                                                                .build()
                                                )
                                        )
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
                                        .users(
                                                of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user1@slac.stanford.edu")
                                                                .build(),
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user2@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                )
        );
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop3")
                                        .description("shop3 user3")
                                        .users(
                                                of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user3@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                )
        );
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop4")
                                        .description("shop4 user[3]")
                                        .users(
                                                of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user3@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                )
        );

    }

    @Test
    public void testFetchAllWorkTypes() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerFindAllWorkTypes(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu")
                        )
                );

        assertThat(newWorkIdResult.getPayload())
                .hasSize(testWorkTypeIds.size())
                .extracting(WorkTypeDTO::id)
                .contains(testWorkTypeIds.toArray(new String[0]));
    }

    @Test
    public void testFetchAllActivityTypes() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerFindAllActivityTypes(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu")
                        )
                );

        assertThat(newWorkIdResult.getPayload())
                .hasSize(testActivityTypeIds.size())
                .extracting(ActivityTypeDTO::id)
                .contains(testActivityTypeIds.toArray(new String[0]));
    }

    @Test
    public void testFetchAllActivityTypeSubTypes() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerFindAllActivitySubTypes(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu")
                        )
                );

        assertThat(newWorkIdResult.getPayload())
                .contains(
                        ActivityTypeSubtypeDTO.BugFix,
                        ActivityTypeSubtypeDTO.DeferredRepair,
                        ActivityTypeSubtypeDTO.Enhancement,
                        ActivityTypeSubtypeDTO.Fabrication,
                        ActivityTypeSubtypeDTO.Inspection,
                        ActivityTypeSubtypeDTO.Installation,
                        ActivityTypeSubtypeDTO.Maintenance,
                        ActivityTypeSubtypeDTO.NewApplication,
                        ActivityTypeSubtypeDTO.Safety,
                        ActivityTypeSubtypeDTO.SoftwareRelease,
                        ActivityTypeSubtypeDTO.Other
                );
    }

    @Test
    public void testCreateNewWork() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();
    }

    @Test
    public void testUpdateWorkOk() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // user1@slac.stanford.edu si the are manager
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        // user[2-3]@slac.stanford.edu are in the shop group
                                        .shopGroupId(testShopGroupIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        // try to update but
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user1@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                .title("work 1 updated")
                                .description("work 1 description updated")
                                .locationId(testLocationIds.get(1))
                                .shopGroupId(testShopGroupIds.get(1))
                                .assignedTo(
                                        List.of("user2@slac.stanford.edu")
                                )
                                .build()
                )
        );

        var updatedWork = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindById(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user1@slac.stanford.edu"),
                        newWorkIdResult.getPayload()
                )
        );

        assertThat(updatedWork).isNotNull();
        assertThat(updatedWork.getPayload()).isNotNull();
        assertThat(updatedWork.getPayload().title()).isEqualTo("work 1 updated");
        assertThat(updatedWork.getPayload().description()).isEqualTo("work 1 description updated");
        assertThat(updatedWork.getPayload().assignedTo()).contains("user2@slac.stanford.edu");
        assertThat(updatedWork.getPayload().location().id()).isEqualTo(testLocationIds.get(1));
        assertThat(updatedWork.getPayload().shopGroup().id()).isEqualTo(testShopGroupIds.get(1));
    }

    @Test
    public void testCreateNewWorkFailNoAuthentication() {
        // create new work
        var notAuthorizedException =
                assertThrows(
                        NotAuthorized.class,
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isUnauthorized(),
                                Optional.empty(),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(notAuthorizedException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void testCheckAuthorizationOnNewlyCreatedWork() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user3@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        // the group contains user1 and user2 and all of them should be reader
                                        .locationId(testLocationIds.get(1))
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.get(1))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        // check authorization on user that create the work
        assertTrue(
                authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        UsernamePasswordAuthenticationToken.authenticated("user1@slac.stanford.edu", "user1@slac.stanford.edu", null),
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload())
                )
        );
        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user2@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization user that creates the work
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user3@slac.stanford.edu",
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization on shop group
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.get(1)),
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);
    }

    @Test
    public void testWorkFindById() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var fullWorkDTO = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newWorkIdResult.getPayload()
                )
        );

        assertThat(fullWorkDTO.getErrorCode()).isEqualTo(0);
        assertThat(fullWorkDTO.getPayload()).isNotNull();
        assertThat(fullWorkDTO.getPayload().id()).isEqualTo(newWorkIdResult.getPayload());
        assertThat(fullWorkDTO.getPayload().accessList())
                .hasSize(3)
                .extracting(AuthorizationResourceDTO::authorizationType)
                .contains(AuthorizationTypeDTO.Write, AuthorizationTypeDTO.Admin, AuthorizationTypeDTO.Admin);

        // read with different user is a reader
        fullWorkDTO = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload()
                )
        );

        assertThat(fullWorkDTO.getErrorCode()).isEqualTo(0);
        assertThat(fullWorkDTO.getPayload()).isNotNull();
        assertThat(fullWorkDTO.getPayload().id()).isEqualTo(newWorkIdResult.getPayload());
        assertThat(fullWorkDTO.getPayload().accessList())
                .hasSize(3)
                .extracting(AuthorizationResourceDTO::authorizationType)
                .contains(AuthorizationTypeDTO.Write, AuthorizationTypeDTO.Read, AuthorizationTypeDTO.Admin);
    }

    @Test
    public void testCreateActivity() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.getFirst())
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.getFirst())
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotNull();
    }

    @Test
    public void testFindAllActivityForWorkId() {
        String[] activityIds = new String[10];
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        .locationId(testLocationIds.getFirst())
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.getFirst())
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();
        for (int i = 0; i < 10; i++) {
            var newActivityIdResult =
                    assertDoesNotThrow(
                            () -> testControllerHelperService.workControllerCreateNew(
                                    mockMvc,
                                    status().isCreated(),
                                    Optional.of("user1@slac.stanford.edu"),
                                    newWorkIdResult.getPayload(),
                                    NewActivityDTO.builder()
                                            .activityTypeId(testActivityTypeIds.getFirst())
                                            .title("New activity 1")
                                            .description("activity 1 description")
                                            .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                            .build()
                            )
                    );
            assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newActivityIdResult.getPayload()).isNotNull();
            activityIds[i] = newActivityIdResult.getPayload();
        }

        // find all activity for work by the admin
        var allActivityFound =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerFindAllActivitiesByWorkId(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                newWorkIdResult.getPayload()
                        )
                );
        assertThat(allActivityFound.getErrorCode()).isEqualTo(0);
        assertThat(allActivityFound.getPayload())
                .hasSize(activityIds.length)
                .extracting(ActivitySummaryDTO::id)
                .contains(activityIds);

                                                                assertThat(allActivityFound.getPayload())
                .hasSize(activityIds.length)
                .extracting(ActivitySummaryDTO::access)
                .containsOnly(AuthorizationTypeDTO.Admin);

        // get all by a normal user (all user should be enabled to read all the activities)
        allActivityFound =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerFindAllActivitiesByWorkId(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user2@slac.stanford.edu"),
                                newWorkIdResult.getPayload()
                        )
                );
        assertThat(allActivityFound.getErrorCode()).isEqualTo(0);
        assertThat(allActivityFound.getPayload())
                .hasSize(activityIds.length)
                .extracting(ActivitySummaryDTO::access)
                .containsOnly(AuthorizationTypeDTO.Read);
    }

    @Test
    public void testCreateActivityByCreator() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user3@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        .locationId(testLocationIds.get(1))
                                        // the group contains user1 and user2 and all of them should be admin
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.get(1))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotNull();
    }

    @Test
    public void testCreateActivityByLocationAreaManager() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user3@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        .locationId(testLocationIds.get(1))
                                        // the group contains user1 and user2 and all of them should be admin
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.get(1))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user2@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotNull();
    }

    @Test
    public void testCreateActivityByShopGroupUsers() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        // the group contains user2 and user3 and all of them should be admin
                                        .locationId(testLocationIds.get(2))
                                        .workTypeId(testWorkTypeIds.get(1))
                                        .shopGroupId(testShopGroupIds.get(2))   // user2 and user3
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResultByUser3 =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.get(2))
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResultByUser3.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResultByUser3.getPayload()).isNotNull();
    }

    @Test
    public void testCreateActivityByAssignedToUsers() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        // the group contains user2 and user3 and all of them should be admin
                                        .locationId(testLocationIds.getFirst())
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.getFirst())
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResultByUser3 =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResultByUser3.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResultByUser3.getPayload()).isNotNull();
    }

    @Test
    public void testUpdateWorkFailOnLocationIdOnNonAdmin() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // user2@slac.stanford.edu si the are manager
                                        // user[1-3]@slac.stanford.edu are in the shop group
                                        .locationId(testLocationIds.get(2))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(2))  // user2 and user3
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        // try to update but
        assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                .locationId(testLocationIds.get(1))
                                .build()
                )
        );
    }

    @Test
    public void testUpdateWorkOkAndCheckForAdminAndGroupReader() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // user2@slac.stanford.edu si the are manager
                                        // user[1-3]@slac.stanford.edu are in the shop group
                                        .locationId(testLocationIds.get(2))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(2))  // user2 and user3
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user2@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization on shop group
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.get(2)),
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // try to update but
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user2@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                .locationId(testLocationIds.getFirst())
                                .shopGroupId(testShopGroupIds.getFirst())
                                .build()
                )
        );

        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user1@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization on shop group
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.getFirst()),
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);
    }

    @Test
    public void testUpdateWorkOkAndCheckForAssignedToChanges() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // user2@slac.stanford.edu si the are manager
                                        // user[1-3]@slac.stanford.edu are in the shop group
                                        .locationId(testLocationIds.get(2))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(2))  // user2 and user3
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user2@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization on shop group
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.get(2)),
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // try to update but
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user2@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                .locationId(testLocationIds.getFirst())
                                .shopGroupId(testShopGroupIds.getFirst())
                                .build()
                )
        );

        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user1@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

        // check authorization on shop group
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.getFirst()),
                        AuthorizationTypeDTO.Read,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);
    }

    @Test
    public void testUpdateWorkOnAssignedToOkByRootAndShopGroupLeader() {
        // create new work
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user1@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // user2@slac.stanford.edu si the are manager
                                        // user[2-3(l)]@slac.stanford.edu are in the shop group
                                        .locationId(testLocationIds.get(2))
                                        .workTypeId(testWorkTypeIds.get(0))
                                        .shopGroupId(testShopGroupIds.get(0))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();


        // try to update by root
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user1@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                .locationId(testLocationIds.getFirst())
                                .shopGroupId(testShopGroupIds.getFirst())
                                // assign to user 2
                                .assignedTo(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );

        // try to update by group leader
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                // group leader cannot update the location
                                //.locationId(testLocationIds.getFirst())
                                .shopGroupId(testShopGroupIds.getFirst())
                                // assign to user 2
                                .assignedTo(List.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );

        // failed to be updated by are manager
        assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isUnauthorized(),
                        // this is the admin fo the location 2
                        Optional.of("user2@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        UpdateWorkDTO.builder()
                                // group leader cannot update the location
                                //.locationId(testLocationIds.getFirst())
                                .shopGroupId(testShopGroupIds.getFirst())
                                // assign to user 2
                                .assignedTo(List.of("user3@slac.stanford.edu"))
                                .build()
                )
        );
    }

    @Test
    public void testUpdateActivityByCreator() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user3@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        .locationId(testLocationIds.get(1))
                                        // the group contains user1 and user2 and all of them should be admin
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.get(1)) // user1 and user2
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var newActivityIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotNull();
        // check saved data
        var fulActivityResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        newActivityIdResult.getPayload()
                )
        );
        assertThat(fulActivityResult.getErrorCode()).isEqualTo(0);
        assertThat(fulActivityResult.getPayload()).isNotNull();
        assertThat(fulActivityResult.getPayload().title()).isEqualTo("New activity 1");
        assertThat(fulActivityResult.getPayload().description()).isEqualTo("activity 1 description");

        // try to update
        assertDoesNotThrow(
                () -> testControllerHelperService.workControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        newActivityIdResult.getPayload(),
                        UpdateActivityDTO.builder()
                                .title("New activity 1 updated")
                                .description("activity 1 description updated")
                                .build()
                )
        );

        var fulActivityUpdateResult = assertDoesNotThrow(
                () -> testControllerHelperService.workControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        newWorkIdResult.getPayload(),
                        newActivityIdResult.getPayload()
                )
        );
        assertThat(fulActivityUpdateResult.getErrorCode()).isEqualTo(0);
        assertThat(fulActivityUpdateResult.getPayload()).isNotNull();
        assertThat(fulActivityUpdateResult.getPayload().title()).isEqualTo("New activity 1 updated");
        assertThat(fulActivityUpdateResult.getPayload().description()).isEqualTo("activity 1 description updated");
    }

    @Test
    public void testUpdateActivityStatusByCreator() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                // this should be admin because is the user that created the work
                                Optional.of("user3@slac.stanford.edu"),
                                NewWorkDTO.builder()
                                        // the location manager is user2@slac.stanford.edu and also this should be admin
                                        .locationId(testLocationIds.get(1))
                                        // the group contains user1 and user2 and all of them should be admin
                                        .workTypeId(testWorkTypeIds.getFirst())
                                        .shopGroupId(testShopGroupIds.get(1))
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotEmpty();

        var newActivityIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().isCreated(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .activityTypeSubtype(ActivityTypeSubtypeDTO.Other)
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotEmpty();

        var updateStatusResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerUpdateStatus(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                newActivityIdResult.getPayload(),
                                UpdateActivityStatusDTO
                                        .builder()
                                        .newStatus(ActivityStatusDTO.Completed)
                                        .build()
                        )
                );
        assertThat(updateStatusResult.getErrorCode()).isEqualTo(0);

        // check the workflow status
        assertThat(
                helperService.checkStatusAndHistoryOnActivity(
                        newActivityIdResult.getPayload(),
                        ImmutableList.of(
                                ActivityStatusDTO.Completed,
                                ActivityStatusDTO.New
                        )
                )
        ).isTrue();
        // work latest status should be review
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkIdResult.getPayload(),
                        ImmutableList.of(
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();

        // try closing the work with an unauthorized user
        var reviewNotAuthorizeOnCreator =
                assertThrows(
                        NotAuthorized.class,
                        () -> testControllerHelperService.workControllerReviewWork(
                                mockMvc,
                                status().isUnauthorized(),
                                Optional.of("user3@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                newActivityIdResult.getPayload(),
                                ReviewWorkDTO
                                        .builder()
                                        .followUpDescription("work has completely finished")
                                        .build()
                        )
                );
        assertThat(reviewNotAuthorizeOnCreator.getErrorCode()).isEqualTo(-1);
        // review the work with location area manager
        var reviewWorkResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerReviewWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user2@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                newActivityIdResult.getPayload(),
                                ReviewWorkDTO
                                        .builder()
                                        .followUpDescription("work has completely finished")
                                        .build()
                        )
                );
        assertThat(reviewWorkResult.getErrorCode()).isEqualTo(0);
        // check the updated workflow states
        assertThat(
                helperService.checkStatusAndHistoryOnWork(
                        newWorkIdResult.getPayload(),
                        ImmutableList.of(
                                WorkStatusDTO.Closed,
                                WorkStatusDTO.Review,
                                WorkStatusDTO.ScheduledJob,
                                WorkStatusDTO.New
                        )
                )
        ).isTrue();
    }

    @Test
    public void getPermittedStatusFromASpecificOne() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerGetPermittedStatus(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user2@slac.stanford.edu"),
                                ActivityStatusDTO.New
                        )
                );

        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload())
                .isNotEmpty()
                .contains(ActivityStatusDTO.Completed, ActivityStatusDTO.Approved, ActivityStatusDTO.Drop, ActivityStatusDTO.Roll);
    }
}
