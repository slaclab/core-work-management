/*
 * -----------------------------------------------------------------------------
 * Title      : ShopGroupControllerTest
 * ----------------------------------------------------------------------------
 * File       : ShopGroupControllerTest.java
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

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.ShopGroup;
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

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ShopGroupControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    private String domainId = null;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        var domainIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerCreateNewDomain(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewDomainDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .workflowImplementations(Set.of("DummyParentWorkflow"))
                                .build()
                )
        );
        assertThat(domainIdResult).isNotNull();
        assertThat(domainIdResult.getPayload()).isNotEmpty();
        domainId = domainIdResult.getPayload();
    }

    @Test
    public void createShopGroupOK() {
        var shopGroupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        NewShopGroupDTO
                                .builder()
                                .name("shopGroup1")
                                .description("shopGroup1 description")
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
        );
        assertThat(shopGroupCreationResult).isNotNull();
        assertThat(shopGroupCreationResult.getPayload()).isNotEmpty();
    }

    @Test
    public void createShopGroupFindByIdOK() {
        var shopGroupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        NewShopGroupDTO
                                .builder()
                                .name("shopGroup1")
                                .description("shopGroup1 description")
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
        );
        assertThat(shopGroupCreationResult).isNotNull();
        assertThat(shopGroupCreationResult.getPayload()).isNotEmpty();

        var getShopGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        shopGroupCreationResult.getPayload()
                )
        );
        assertThat(getShopGroupResult.getPayload()).isNotNull();
        assertThat(getShopGroupResult.getPayload().name()).isEqualTo("shopGroup1");
    }

    @Test
    public void updateShopGroup() {
        var shopGroupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        NewShopGroupDTO
                                .builder()
                                .name("shopGroup1")
                                .description("shopGroup1 description")
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
        );
        assertThat(shopGroupCreationResult).isNotNull();
        assertThat(shopGroupCreationResult.getPayload()).isNotEmpty();

        // update using a root user
        assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        shopGroupCreationResult.getPayload(),
                        UpdateShopGroupDTO
                                .builder()
                                .name("shopGroup1 updated")
                                .description("shopGroup1 description updated")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user3@slac.stanford.edu")
                                                        // this new user will be an admin of the group
                                                        .isLeader(true)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        var getShopGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        shopGroupCreationResult.getPayload()
                )
        );
        assertThat(getShopGroupResult.getPayload()).isNotNull();
        assertThat(getShopGroupResult.getPayload().name()).isEqualTo("shopGroup1 updated");
        assertThat(getShopGroupResult.getPayload().description()).isEqualTo("shopGroup1 description updated");
        assertThat(getShopGroupResult.getPayload().users())
                .hasSize(1)
                .extracting(ShopGroupUserDTO::user)
                .extracting(PersonDTO::mail)
                .contains("user3@slac.stanford.edu");

        // update using a group leader
        assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerUpdate(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        domainId,
                        shopGroupCreationResult.getPayload(),
                        UpdateShopGroupDTO
                                .builder()
                                .name("shopGroup updated")
                                .description("shopGroup description updated")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user2@slac.stanford.edu")
                                                        // this new user will be an admin of the group
                                                        .isLeader(true)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        getShopGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        shopGroupCreationResult.getPayload()
                )
        );
        assertThat(getShopGroupResult.getPayload()).isNotNull();
        assertThat(getShopGroupResult.getPayload().name()).isEqualTo("shopGroup updated");
        assertThat(getShopGroupResult.getPayload().description()).isEqualTo("shopGroup description updated");
        assertThat(getShopGroupResult.getPayload().users())
                .hasSize(1)
                .extracting(ShopGroupUserDTO::user)
                .extracting(PersonDTO::mail)
                .contains("user2@slac.stanford.edu");
    }

    @Test
    public void findAllOK() {
        var shopGroupCreation1Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        NewShopGroupDTO
                                .builder()
                                .name("shopGroup1")
                                .description("shopGroup1 description")
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
        );
        assertThat(shopGroupCreation1Result).isNotNull();
        assertThat(shopGroupCreation1Result.getPayload()).isNotEmpty();

        var shopGroupCreation2Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId,
                        NewShopGroupDTO
                                .builder()
                                .name("shopGroup2")
                                .description("shopGroup2 description")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user1@slac.stanford.edu")
                                                        .build(),
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user3@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(shopGroupCreation2Result).isNotNull();
        assertThat(shopGroupCreation2Result.getPayload()).isNotEmpty();

        var findAllResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        domainId
                )
        );
        assertThat(findAllResult.getErrorCode()).isEqualTo(0);
        assertThat(findAllResult.getPayload())
                .extracting(ShopGroupDTO::name)
                .contains("shopGroup1", "shopGroup2");
    }
}
