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

import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserInputDTO;
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
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), ShopGroup.class);
    }

    @Test
    public void createShopGroupOK() {
        var shopGroupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
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
                        shopGroupCreationResult.getPayload()
                )
        );
        assertThat(getShopGroupResult.getPayload()).isNotNull();
        assertThat(getShopGroupResult.getPayload().name()).isEqualTo("shopGroup1");
    }

    @Test
    public void findAllOK() {
        var shopGroupCreation1Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
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
                () ->testControllerHelperService.shopGroupControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(findAllResult.getErrorCode()).isEqualTo(0);
        assertThat(findAllResult.getPayload())
                .extracting(ShopGroupDTO::name)
                .contains("shopGroup1", "shopGroup2");
    }
}
