package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound;
import edu.stanford.slac.ad.eed.baselib.exception.UserNotFound;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
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

import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ShopGroupServiceTest {
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), ShopGroup.class);
    }

    @Test
    public void createNewOk() {
        var newShopId = assertDoesNotThrow(
                () -> shopGroupService.createNew(
                        NewShopGroupDTO
                                .builder()
                                .name("name")
                                .description("description")
                                .usersEmails(Set.of("user1@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newShopId).isNotNull();
    }

    @Test
    public void createNewFailsWithNonExistingUserEmail() {
        var userNotFoundException = assertThrows(
                PersonNotFound.class,
                () -> shopGroupService.createNew(
                        NewShopGroupDTO
                                .builder()
                                .name("name")
                                .description("description")
                                .usersEmails(Set.of("bad-user@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(userNotFoundException).isNotNull();
    }
}
