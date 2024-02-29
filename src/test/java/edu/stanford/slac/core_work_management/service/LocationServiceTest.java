package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.exception.LocationNotFound;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocationServiceTest {
    @Autowired
    private LocationService locationService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private Validator validator;
    private final List<String> shopGroupIds = new ArrayList<>();

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        shopGroupIds.add(
                shopGroupService.createNew(
                        NewShopGroupDTO.builder()
                                .name("shop1")
                                .description("shop1 user[2-3]")
                                .userEmails(of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );
        shopGroupIds.add(
                shopGroupService.createNew(
                        NewShopGroupDTO.builder()
                                .name("shop2")
                                .description("shop1 user[1-2]")
                                .userEmails(of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Location.class);
    }

    @Test
    public void testCreateNewLocation() {
        var newLocationId = assertDoesNotThrow(
                () -> locationService.createNew(
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .locationShopGroupId(shopGroupIds.getFirst())
                                .build()
                )
        );

        assertThat(newLocationId).isNotNull();
        var newCreatedLocation = assertDoesNotThrow(
                () -> locationService.findById(
                        newLocationId
                )
        );
        assertThat(newCreatedLocation).isNotNull();
        assertThat(newCreatedLocation.id()).isEqualTo(newLocationId);
    }

    @Test
    public void testFindAllWithoutFilter() {
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            var newLocationId = assertDoesNotThrow(
                    () -> locationService.createNew(
                            NewLocationDTO.builder()
                                    .name(String.format("%d_text", finalIdx))
                                    .description(String.format("%d_text", finalIdx))
                                    .locationManagerUserId("user1@slac.stanford.edu")
                                    .locationShopGroupId(shopGroupIds.getFirst())
                                    .build()
                    )
            );
            assertThat(newLocationId).isNotNull();
        }

        var foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        LocationFilterDTO.builder().build()
                )
        );
        assertThat(foundLocations).isNotNull().hasSize(100);

        foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        LocationFilterDTO
                                .builder()
                                .text("1_text")
                                .build()
                )
        );
        assertThat(foundLocations).isNotNull().hasSize(1);

        foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        LocationFilterDTO
                                .builder()
                                .text("1_text 2_text")
                                .build()
                )
        );
        assertThat(foundLocations).isNotNull().hasSize(2);
    }

    @Test
    public void testLocationWithParentOK() {
        var newLocationId = assertDoesNotThrow(
                () -> locationService.createNew(
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .locationShopGroupId(shopGroupIds.getFirst())
                                .build()
                )
        );
        var newLocationWithParentId = assertDoesNotThrow(
                () -> locationService.createNew(
                        NewLocationDTO.builder()
                                .name("test child")
                                .description("test")
                                .parentId(newLocationId)
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .locationShopGroupId(shopGroupIds.getFirst())
                                .build()
                )
        );
        assertThat(newLocationWithParentId).isNotNull();
        var newCreatedLocation = assertDoesNotThrow(
                () -> locationService.findById(
                        newLocationWithParentId
                )
        );
        assertThat(newCreatedLocation).isNotNull();
        assertThat(newCreatedLocation.id()).isEqualTo(newLocationWithParentId);
        assertThat(newCreatedLocation.parentId()).isEqualTo(newLocationId);
    }

    @Test
    public void testErrorCreatingLocationWithNotFoundParent() {
        var locationNotFoundForParent = assertThrows(
                LocationNotFound.class,
                () -> locationService.createNew(
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .parentId("bad id")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .locationShopGroupId(shopGroupIds.getFirst())
                                .build()
                )
        );
        assertThat(locationNotFoundForParent).isNotNull();
    }

    @Test
    public void testErrorOnNameDescriptionAndExternalLocation() {
        // Test when externalLocationIdentifier is null and name and description are not empty
        NewLocationDTO newLocationDTO = new NewLocationDTO(
                null,
                "test",
                "test",
                null,
                "user1@slac.stanford.edu",
                shopGroupIds.getFirst()
        );
        Set<ConstraintViolation<NewLocationDTO>> violations = validator.validate(newLocationDTO);
        assertTrue(violations.isEmpty());

        // Test when externalLocationIdentifier is not empty and name and description are empty
        newLocationDTO = new NewLocationDTO(
                null,
                null,
                null,
                "external id",
                "user1@slac.stanford.edu",
                shopGroupIds.getFirst()
        );
        violations = validator.validate(newLocationDTO);
        assertTrue(violations.isEmpty());

        // Test when externalLocationIdentifier is not empty and name and description are not empty
        newLocationDTO = new NewLocationDTO(
                null,
                "test",
                "test",
                "external id",
                "user1@slac.stanford.edu",
                shopGroupIds.getFirst()
        );
        violations = validator.validate(newLocationDTO);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void testErrorOrParentId() {
        // Test when externalLocationIdentifier is null and name and description are not empty
        NewLocationDTO newLocationDTO = new NewLocationDTO(
                "",
                "test",
                "test",
                null,
                "user1@slac.stanford.edu",
                shopGroupIds.getFirst()
        );
        Set<ConstraintViolation<NewLocationDTO>> violations = validator.validate(newLocationDTO);
        assertThat(violations).hasSize(2);
    }
}
