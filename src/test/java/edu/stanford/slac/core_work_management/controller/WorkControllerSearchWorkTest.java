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

import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
public class WorkControllerSearchWorkTest {
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
    DomainService domainService;
    @Autowired
    private WorkService workService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private LOVService lovService;

    private DomainDTO domainDTO;
    private WorkflowDTO workflowDTO;
    private final List<String> testShopGroupIds = new ArrayList<>();
    private final List<String> testLocationIds = new ArrayList<>();
    private final List<String> testWorkTypeIds = new ArrayList<>();
    private List<LOVElementDTO> projectLovValues;
    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        domainDTO = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("domain1")
                                .description("domain1 description")
                                .workflowImplementations(
                                        of("DummyParentWorkflow")
                                )
                                .build()
                )
        );
        assertThat(domainDTO).isNotNull();
        workflowDTO = domainDTO.workflows().stream().findFirst().get();
        assertThat(workflowDTO).isNotNull();

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
                                        .workflowId(workflowDTO.id())
                                        .validatorName("validation/DummyParentValidation.groovy")
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
                                        .workflowId(workflowDTO.id())
                                        .validatorName("validation/DummyParentValidation.groovy")
                                        .build()
                        )
                )
        );
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), Authorization.class);

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

    }

    @Test
    public void testSearchForward() {
        List<String> workIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // create new work
            int finalI = i;
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
                                            .title("work %s".formatted(finalI))
                                            .description("work %s description".formatted(finalI))
                                            .build()
                            )
                    );
            assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newWorkIdResult.getPayload()).isNotNull();
            workIds.add(newWorkIdResult.getPayload());
        }

        Optional<String> anchorIdOptional = Optional.empty();
        // tri to find all going forward
        for (int i = 0; i < 10; i++) {
            Optional<String> finalAnchorIdOptional = anchorIdOptional;
            var searchResult =
                    assertDoesNotThrow(
                            () -> testControllerHelperService.workControllerSearchAllWork(
                                    mockMvc,
                                    status().isOk(),
                                    Optional.of("user1@slac.stanford.edu"),
                                    finalAnchorIdOptional,
                                    Optional.empty(),
                                    Optional.of(10),
                                    Optional.empty()
                            )
                    );
            if (searchResult.getPayload().isEmpty()) {
                break;
            }

            assertThat(searchResult.getErrorCode()).isEqualTo(0);
            assertThat(searchResult.getPayload()).hasSize(10);
            for(int i1 = 0; i1 < 10; i1++) {
                assertThat(searchResult.getPayload().get(i1).id()).isEqualTo(workIds.get(i1 + i * 10));
            }
            anchorIdOptional = Optional.of(searchResult.getPayload().get(9).id());
        }
    }

    @Test
    public void testSearchBackward() {
        List<String> workIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // create new work
            int finalI = i;
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
                                            .title("work %s".formatted(finalI))
                                            .description("work %s description".formatted(finalI))
                                            .build()
                            )
                    );
            assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newWorkIdResult.getPayload()).isNotNull();
            workIds.add(newWorkIdResult.getPayload());
        }

        // tri to find all going backward
        for (int i = 0; i < 10; i++) {
            Optional<String> finalAnchorIdOptional = Optional.of(workIds.get(99 - i * 10));
            var searchResult =
                    assertDoesNotThrow(
                            () -> testControllerHelperService.workControllerSearchAllWork(
                                    mockMvc,
                                    status().isOk(),
                                    Optional.of("user1@slac.stanford.edu"),
                                    finalAnchorIdOptional,
                                    Optional.of(10),
                                    Optional.empty(),
                                    Optional.empty()
                            )
                    );
            if (searchResult.getPayload().isEmpty()) {
                break;
            }

            assertThat(searchResult.getErrorCode()).isEqualTo(0);
            assertThat(searchResult.getPayload()).hasSize(10);
            for(int i1 = 0; i1 < 10; i1++) {
                assertThat(searchResult.getPayload().get(9-i1).id()).isEqualTo(workIds.get(99 - i * 10 - i1));
            }
        }
    }

    @Test
    public void testSearchByTextFilter() {
        List<String> workIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // create new work
            int finalI = i;
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
                                            .title("work %s".formatted(finalI))
                                            .description("work %s description".formatted(finalI))
                                            .build()
                            )
                    );
            assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newWorkIdResult.getPayload()).isNotNull();
            workIds.add(newWorkIdResult.getPayload());
        }

        var searchResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerSearchAllWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(10),
                                Optional.of("51 50")
                        )
                );
        assertThat(searchResult.getPayload())
                .hasSize(2)
                .extracting(WorkDTO::title)
                .containsExactlyInAnyOrder("work 50", "work 51");
    }
}
