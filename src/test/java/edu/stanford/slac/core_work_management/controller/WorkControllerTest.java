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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.*;
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
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
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
    private DomainService domainService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private LOVService lovService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    private DomainDTO domainDTO;
    private WorkflowDTO workflowDTO;
    private final List<String> testShopGroupIds = new ArrayList<>();
    private final List<String> testLocationIds = new ArrayList<>();
    private final List<String> testWorkTypeIds = new ArrayList<>();
    private final List<String> testActivityTypeIds = new ArrayList<>();
    private List<LOVElementDTO> projectLovValues;
    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);

        domainDTO = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("domain1")
                                .description("domain1 description")
                                .workflowImplementations(
                                        of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domainDTO).isNotNull();
        workflowDTO = domainDTO.workflows().stream().findFirst().get();
        assertThat(workflowDTO).isNotNull();

        // create location for test
        testLocationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                        () -> domainService.createNew(
                                domainDTO.id(),
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type 1")
                                        .description("Work type 1 description")
                                        .validatorName("validation/DummyParentValidation.groovy")
                                        .workflowId(workflowDTO.id())
                                        .build()
                        )
                )
        );
        // create work 2
        testWorkTypeIds.add(
                assertDoesNotThrow(
                        () -> domainService.createNew(
                                domainDTO.id(),
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type 2")
                                        .description("Work type 2 description")
                                        .validatorName("validation/DummyParentValidation.groovy")
                                        .workflowId(workflowDTO.id())
                                        .build()
                        )
                )
        );

    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        // shop group creation need to be moved here because the manage the authorization for the leader
        testShopGroupIds.clear();
        testShopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                        () -> testControllerHelperService.domainControllerFindAllWorkTypes(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                domainDTO.id()
                        )
                );

        assertThat(newWorkIdResult.getPayload())
                .hasSize(testWorkTypeIds.size())
                .extracting(WorkTypeDTO::id)
                .contains(testWorkTypeIds.toArray(new String[0]));
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                        domainDTO.id(),
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
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        // this is the admin fo the location 2
                        Optional.of("user1@slac.stanford.edu"),
                        domainDTO.id(),
                        newWorkIdResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                                domainDTO.id(),
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
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainDTO.id(),
                        newWorkIdResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
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
                () -> testControllerHelperService.workControllerFindWorkById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        domainDTO.id(),
                        newWorkIdResult.getPayload(),
                        WorkDetailsOptionDTO.builder().build()
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
}
