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
import edu.stanford.slac.core_work_management.migration.M6_IndexForWorkStatistic;
import edu.stanford.slac.core_work_management.migration.M7_IndexForExtendedWorkSearch;
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

import java.util.*;

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
    private DomainDTO alternateDomainDTO;
    private WorkflowDTO workflowDTO;
    private final List<String> testShopGroupIds = new ArrayList<>();
    private final List<String> testLocationIds = new ArrayList<>();
    private final List<String> testWorkTypeIds = new ArrayList<>();

    private final List<String> testAlternateShopGroupIds = new ArrayList<>();
    private final List<String> testAlternateLocationIds = new ArrayList<>();
    private final List<String> testAlternateWorkTypeIds = new ArrayList<>();
    private List<LOVElementDTO> projectLovValues;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);

        // init index
        M6_IndexForWorkStatistic m6_indexForWorkStatistic = new M6_IndexForWorkStatistic(mongoTemplate);
        assertDoesNotThrow(m6_indexForWorkStatistic::changeSet);
        M7_IndexForExtendedWorkSearch m7_indexForExtendedWorkSearch = new M7_IndexForExtendedWorkSearch(mongoTemplate);
        assertDoesNotThrow(m7_indexForExtendedWorkSearch::changeSet);

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

        alternateDomainDTO = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("domain2")
                                .description("domain2 description")
                                .workflowImplementations(
                                        of("DummyParentWorkflow")
                                )
                                .build()
                )
        );
        assertThat(alternateDomainDTO).isNotNull();

        // create shop groups
        createShopGroup(domainDTO, testShopGroupIds, null);
        createShopGroup(alternateDomainDTO, testAlternateShopGroupIds, "alternate");

        // create location for test
        createLocation(domainDTO, testLocationIds, null);
        createLocation(alternateDomainDTO, testAlternateLocationIds, "alternate");

        // create work types
        createWorkTypes(domainDTO, testWorkTypeIds, null);
        createWorkTypes(alternateDomainDTO, testAlternateWorkTypeIds, "alternate");
    }

    private void createWorkTypes(DomainDTO domainDTO, List<String> workTypeIds, String postfix) {
        workflowDTO = domainDTO.workflows().stream().findFirst().get();
        assertThat(workflowDTO).isNotNull();
        // create work 1
        workTypeIds.add(
                assertDoesNotThrow(
                        () -> domainService.createNew(
                                domainDTO.id(),
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type %s 1".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("Work type %s 1 description".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .workflowId(workflowDTO.id())
                                        .validatorName("validation/DummyParentValidation.groovy")
                                        .build()
                        )
                )
        );
        // create work 2
        workTypeIds.add(
                assertDoesNotThrow(
                        () -> domainService.createNew(
                                domainDTO.id(),
                                NewWorkTypeDTO
                                        .builder()
                                        .title("Work type %s 2".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("Work type %s 2 description".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .workflowId(workflowDTO.id())
                                        .validatorName("validation/DummyParentValidation.groovy")
                                        .build()
                        )
                )
        );
    }

    private void createLocation(DomainDTO domainDTO, List<String> locationIds, String postfix) {
        workflowDTO = domainDTO.workflows().stream().findFirst().get();
        assertThat(workflowDTO).isNotNull();
        locationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                domainDTO.id(),
                                NewLocationDTO.builder()
                                        .name("location %s 1".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("location %s 1 description".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .locationManagerUserId("user1@slac.stanford.edu")
                                        .build()
                        )
                )
        );
        locationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                domainDTO.id(),
                                NewLocationDTO.builder()
                                        .name("location %s 2".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("location %s 2 description".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .locationManagerUserId("user2@slac.stanford.edu")
                                        .build()
                        )
                )
        );
        locationIds.add(
                assertDoesNotThrow(
                        () -> locationService.createNew(
                                domainDTO.id(),
                                NewLocationDTO.builder()
                                        .name("location %s 3".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("location %s 3 description".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .locationManagerUserId("user2@slac.stanford.edu")
                                        .build()
                        )
                )
        );
    }

    private void createShopGroup(DomainDTO domainDTO, List<String> shopGroupIds, String postfix) {
        workflowDTO = domainDTO.workflows().stream().findFirst().get();
        assertThat(workflowDTO).isNotNull();
        shopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainDTO.id(),
                                NewShopGroupDTO.builder()
                                        .name("shop group %s 1".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("shop group %s 1 description".formatted(Objects.requireNonNullElse(postfix, "")))
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
        shopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainDTO.id(),
                                NewShopGroupDTO.builder()
                                        .name("shop group %s 2".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("shop group %s 2 description".formatted(Objects.requireNonNullElse(postfix, "")))
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
        shopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainDTO.id(),
                                NewShopGroupDTO.builder()
                                        .name("shop group %s 3".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("shop group %s 3 description".formatted(Objects.requireNonNullElse(postfix, "")))
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
        shopGroupIds.add(
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainDTO.id(),
                                NewShopGroupDTO.builder()
                                        .name("shop group %s 4".formatted(Objects.requireNonNullElse(postfix, "")))
                                        .description("shop group %s 4 description".formatted(Objects.requireNonNullElse(postfix, "")))
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
            for (int i1 = 0; i1 < 10; i1++) {
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
            for (int i1 = 0; i1 < 10; i1++) {
                assertThat(searchResult.getPayload().get(9 - i1).id()).isEqualTo(workIds.get(99 - i * 10 - i1));
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
                .extracting(WorkSummaryDTO::title)
                .containsExactlyInAnyOrder("work 50", "work 51");
    }

    @Test
    public void testSearchByDomainId() {
        List<String> workIdsDomain1 = new ArrayList<>();
        List<String> workIdsDomain2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // create new work
            int finalI = i;
            // select a random number form one and two
            int domainIndex = i % 2;
            var newWorkIdResult =
                    assertDoesNotThrow(
                            () -> testControllerHelperService.workControllerCreateNew(
                                    mockMvc,
                                    status().isCreated(),
                                    Optional.of("user1@slac.stanford.edu"),
                                    domainIndex == 0 ? domainDTO.id() : alternateDomainDTO.id(),
                                    NewWorkDTO.builder()
                                            .locationId(domainIndex == 0 ? testLocationIds.get(0) : testAlternateLocationIds.get(0))
                                            .workTypeId(domainIndex == 0 ? testWorkTypeIds.get(0) : testAlternateWorkTypeIds.get(0))
                                            .shopGroupId(domainIndex == 0 ? testShopGroupIds.get(0) : testAlternateShopGroupIds.get(0))
                                            .title("work %s".formatted(finalI))
                                            .description("work %s description".formatted(finalI))
                                            .build()
                            )
                    );
            assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newWorkIdResult.getPayload()).isNotNull();
            if(domainIndex == 0) {
                workIdsDomain1.add(newWorkIdResult.getPayload());
            } else {
                workIdsDomain2.add(newWorkIdResult.getPayload());
            }
        }
        // search all the work for domain 1
        var searchResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerSearchAllWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(100),
                                Optional.empty(),
                                Optional.of(List.of(domainDTO.id())),
                                Optional.empty()
                        )
                );
        assertThat(searchResult.getPayload()).isNotEmpty().hasSize(50);
        searchResult.getPayload().forEach(workSummaryDTO -> {
            assertThat(workIdsDomain1).contains(workSummaryDTO.id());
        });

        // search all the work for domain 2
        searchResult =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerSearchAllWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(100),
                                Optional.empty(),
                                Optional.of(List.of(alternateDomainDTO.id())),
                                Optional.empty()
                        )
                );
        assertThat(searchResult.getPayload()).isNotEmpty().hasSize(50);
        searchResult.getPayload().forEach(workSummaryDTO -> {
            assertThat(workIdsDomain2).contains(workSummaryDTO.id());
        });
    }

    @Test
    public void testSearchByWorkType() {
        Set<String> workIdsDomain1 = new HashSet<>();
        Set<String> workTypeIdDomain1 = new HashSet<>();
        Set<String> workIdsDomain2 = new HashSet<>();
        Set<String> workTypeIdDomain2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            // create new work
            int finalI = i;
            // select a random number form one and two
            int domainIndex = i % 2;
            int workTypeIndex = i % 2;
            var newWorkIdResult =
                    assertDoesNotThrow(
                            () -> testControllerHelperService.workControllerCreateNew(
                                    mockMvc,
                                    status().isCreated(),
                                    Optional.of("user1@slac.stanford.edu"),
                                    domainIndex == 0 ? domainDTO.id() : alternateDomainDTO.id(),
                                    NewWorkDTO.builder()
                                            .locationId(domainIndex == 0 ? testLocationIds.get(0) : testAlternateLocationIds.get(0))
                                            .workTypeId(domainIndex == 0 ? testWorkTypeIds.get(0) : testAlternateWorkTypeIds.get(0))
                                            .shopGroupId(domainIndex == 0 ? testShopGroupIds.get(0) : testAlternateShopGroupIds.get(0))
                                            .title("work %s".formatted(finalI))
                                            .description("work %s description".formatted(finalI))
                                            .build()
                            )
                    );
            assertThat(newWorkIdResult.getErrorCode()).isEqualTo(0);
            assertThat(newWorkIdResult.getPayload()).isNotNull();
            if(domainIndex == 0) {
                workIdsDomain1.add(newWorkIdResult.getPayload());
                workTypeIdDomain1.add(testWorkTypeIds.get(0));
            } else {
                workIdsDomain2.add(newWorkIdResult.getPayload());
                workTypeIdDomain2.add(testAlternateWorkTypeIds.get(0));
            }
        }
        // // fetch all work type 1 from domain 1
        var searchResultWT1D1 =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerSearchAllWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(100),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(List.of(testWorkTypeIds.get(0))),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        )
                );
        assertThat(searchResultWT1D1.getPayload()).isNotEmpty().hasSizeLessThanOrEqualTo(50);
        searchResultWT1D1.getPayload().forEach(workSummaryDTO -> {
            assertThat(workIdsDomain1).contains(workSummaryDTO.id());
            assertThat(workTypeIdDomain1).contains(workSummaryDTO.workType().id());
        });

        var searchResultWT1D2 =
                assertDoesNotThrow(
                        () -> testControllerHelperService.workControllerSearchAllWork(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(100),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(List.of(testAlternateWorkTypeIds.get(0))),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        )
                );
        assertThat(searchResultWT1D2.getPayload()).isNotEmpty().hasSizeLessThanOrEqualTo(50);
        searchResultWT1D2.getPayload().forEach(workSummaryDTO -> {
            assertThat(workIdsDomain2).contains(workSummaryDTO.id());
            assertThat(workTypeIdDomain2).contains(workSummaryDTO.workType().id());
        });
    }
}
