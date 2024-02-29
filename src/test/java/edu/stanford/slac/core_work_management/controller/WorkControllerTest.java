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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

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
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                NewShopGroupDTO.builder()
                                        .name("shop3")
                                        .description("shop3 user3")
                                        .userEmails(of("user3@slac.stanford.edu"))
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
                                        .userEmails(of("user3@slac.stanford.edu"))
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
                                        .locationManagerUserId("user2@slac.stanford.edu")
                                        .locationShopGroupId(testShopGroupIds.get(1))
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
                                        .locationShopGroupId(testShopGroupIds.get(2))
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
        // create activity type for work 2
        testActivityTypeIds.put(testWorkTypeIds.get(1), new ArrayList<>());
        testActivityTypeIds.get(testWorkTypeIds.get(1)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(1),
                                NewActivityTypeDTO
                                        .builder()
                                        .title("Activity 1 work type 2")
                                        .description("Activity 1 description")
                                        .build()
                        )
                )
        );
        testActivityTypeIds.get(testWorkTypeIds.get(1)).add(
                assertDoesNotThrow(
                        () -> workService.ensureActivityType(
                                testWorkTypeIds.get(1),
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
        mongoTemplate.remove(new Query(), Authorization.class);

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

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
                                        .title("work 1")
                                        .description("work 1 description")
                                        .build()
                        )
                );
        assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newWorkIdResult.getPayload()).isNotNull();
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
                                        .locationId(testLocationIds.get(1))
                                        // the group contains user1 and user2 and all of them should be admin
                                        .workTypeId(testWorkTypeIds.getFirst())
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
                        UsernamePasswordAuthenticationToken.authenticated("user3@slac.stanford.edu", "user1@slac.stanford.edu", null),
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload())
                )
        );
        // check authorization on location manager
        assertThat(
                authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                        "user3@slac.stanford.edu",
                        AuthorizationTypeDTO.Admin,
                        WORK_AUTHORIZATION_TEMPLATE.formatted(newWorkIdResult.getPayload()),
                        Optional.empty()
                )
        ).hasSize(1);

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
                        SHOP_GROUP_FAKE_USER_TEMPLATE.formatted(testShopGroupIds.get(1)),
                        AuthorizationTypeDTO.Admin,
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
                                        .locationId(testLocationIds.get(0))
                                        .workTypeId(testWorkTypeIds.get(0))
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
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.get(0)).get(0))
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .build()
                        )
                );
        assertThat(newActivityIdResult.getErrorCode()).isEqualTo(0);
        assertThat(newActivityIdResult.getPayload()).isNotNull();
    }

    @Test
    public void testCreateActivityExceptionOnWrongActivityType() {
        var newWorkIdResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerCreateNew(
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
        assertThat(newWorkIdResult.getPayload()).isNotNull();

        var exceptionForWrongActivityType =
                assertThrows(
                        ControllerLogicException.class,
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().is5xxServerError(),
                                Optional.of("user1@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.get(1)).get(0))
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .build()
                        )
                );
        assertThat(exceptionForWrongActivityType.getErrorCode()).isEqualTo(-3);

        var exceptionForNotFoundActivityType =
                assertThrows(
                        ActivityTypeNotFound.class,
                        () -> testControllerHelperService.workControllerCreateNew(
                                mockMvc,
                                status().is4xxClientError(),
                                Optional.of("user1@slac.stanford.edu"),
                                newWorkIdResult.getPayload(),
                                NewActivityDTO.builder()
                                        .activityTypeId("not-found-activity-type-id")
                                        .title("New activity 1")
                                        .description("activity 1 description")
                                        .build()
                        )
                );
        assertThat(exceptionForNotFoundActivityType.getErrorCode()).isEqualTo(-2);
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
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.getFirst()).getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
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
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.getFirst()).getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
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
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.get(1)).getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
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
                                        .title("work 1")
                                        .description("work 1 description")
                                        .assignedTo(List.of("user3@slac.stanford.edu"))
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
                                        .activityTypeId(testActivityTypeIds.get(testWorkTypeIds.getFirst()).getFirst())
                                        .title("New activity 1")
                                        .description("activity 1 description")
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
    public void testUpdateWorkOk() {
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
}
