package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.LocationNotFound;
import edu.stanford.slac.core_work_management.model.Domain;
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
    private DomainService domainService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private Validator validator;

    private String domainId;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        domainId = domainService.createNew(
                NewDomainDTO.builder()
                        .name("test")
                        .description("test")
                        .workflowImplementations(of("DummyParentWorkflow"))
                        .build()
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
                        domainId,
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );

        assertThat(newLocationId).isNotNull();
        var newCreatedLocation = assertDoesNotThrow(
                () -> locationService.findById(
                        domainId,
                        newLocationId
                )
        );
        assertThat(newCreatedLocation).isNotNull();
        assertThat(newCreatedLocation.id()).isEqualTo(newLocationId);
        assertThat(newCreatedLocation.domain().id()).isEqualTo(domainId);
    }

    @Test
    public void testFindAllWithoutFilter() {
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            var newLocationId = assertDoesNotThrow(
                    () -> locationService.createNew(
                            domainId,
                            NewLocationDTO.builder()
                                    .name(String.format("%d_text", finalIdx))
                                    .description(String.format("%d_text", finalIdx))
                                    .locationManagerUserId("user1@slac.stanford.edu")
                                    .build()
                    )
            );
            assertThat(newLocationId).isNotNull();
        }

        var foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        domainId, LocationFilterDTO.builder().build()
                )
        );
        assertThat(foundLocations).isNotNull().hasSize(100);

        foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        domainId, LocationFilterDTO
                                .builder()
                                .text("1_text")
                                .build()
                )
        );
        assertThat(foundLocations).isNotNull().hasSize(1);

        foundLocations = assertDoesNotThrow(
                () -> locationService.findAll(
                        domainId, LocationFilterDTO
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
                        domainId,
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        var newLocationWithParentId = assertDoesNotThrow(
                () -> locationService.createNewChild(
                        domainId,
                        newLocationId,
                        NewLocationDTO.builder()
                                .name("test child")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(newLocationWithParentId).isNotNull();
        var newCreatedLocation = assertDoesNotThrow(
                () -> locationService.findById(
                        domainId,
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
                () -> locationService.createNewChild(
                        domainId,
                        "bad-id",
                        NewLocationDTO.builder()
                                .name("test")
                                .description("test")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(locationNotFoundForParent).isNotNull();
    }

    @Test
    public void testErrorOnNameDescriptionAndExternalLocation() {
        // Test when externalLocationIdentifier is null and name and description are not empty
        NewLocationDTO newLocationDTO = new NewLocationDTO(
                "test",
                "test",
                null,
                "user1@slac.stanford.edu"
        );
        Set<ConstraintViolation<NewLocationDTO>> violations = validator.validate(newLocationDTO);
        assertTrue(violations.isEmpty());

        // Test when externalLocationIdentifier is not empty and name and description are empty
        newLocationDTO = new NewLocationDTO(
                null,
                null,
                "external id",
                "user1@slac.stanford.edu"
        );
        violations = validator.validate(newLocationDTO);
        assertTrue(violations.isEmpty());

        // Test when externalLocationIdentifier is not empty and name and description are not empty
        newLocationDTO = new NewLocationDTO(
                "test",
                "test",
                "external id",
                "user1@slac.stanford.edu"
        );
        violations = validator.validate(newLocationDTO);
        assertFalse(violations.isEmpty());
    }
}
