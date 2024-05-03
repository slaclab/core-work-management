package edu.stanford.slac.core_work_management.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.cis_api.api.InventoryElementControllerApi;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryDomainSummaryDTO;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryElementSummaryDTO;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.exception.ShopGroupNotFound;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
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
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private DomainService domainService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private InventoryElementControllerApi inventoryElementControllerApi;

    private String domainId;
    private final List<String> shopGroupIds = new ArrayList<>();
    private InventoryDomainSummaryDTO inventoryDomainDTO;
    private List<InventoryElementSummaryDTO> inventoryElementSummaryDTOList;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        domainId = domainService.createNew(
                NewDomainDTO.builder()
                        .name("domain1")
                        .description("domain1 description")
                        .build()
        );
        assertThat(domainId).isNotEmpty();

        shopGroupIds.add(
                shopGroupService.createNew(
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
        );
        shopGroupIds.add(
                shopGroupService.createNew(
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
        );
        // fetch all inventory elements from demo
        var foundDomainsResult = inventoryElementControllerApi.findAllDomain();
        assertThat(foundDomainsResult.getErrorCode()).isEqualTo(0);
        assertThat(foundDomainsResult.getPayload())
                .isNotNull()
                .size()
                .isGreaterThan(0);
        // get the first domain to test
        inventoryDomainDTO = Objects.requireNonNull(foundDomainsResult.getPayload()).getFirst();

        var foundElementsResult = inventoryElementControllerApi.findAllElements(
                inventoryDomainDTO.getId(),
                null,
                null,
                10,
                null,
                null,
                null
        );
        assertThat(foundElementsResult.getErrorCode()).isEqualTo(0);
        assertThat(foundElementsResult.getPayload())
                .isNotNull()
                .size()
                .isGreaterThan(0);
        inventoryElementSummaryDTOList = foundElementsResult.getPayload();
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Location.class);
    }


    @Test
    public void createNewStandaloneLocation() {
        var exceptionOnMandatoryField = assertThrows(
                MethodArgumentNotValidException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .build()
                )
        );
        assertThat(exceptionOnMandatoryField).isNotNull();
        exceptionOnMandatoryField = assertThrows(
                MethodArgumentNotValidException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .name("location1")
                                .build()
                )
        );
        assertThat(exceptionOnMandatoryField).isNotNull();
        exceptionOnMandatoryField = assertThrows(
                MethodArgumentNotValidException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .description("location1 description")
                                .build()
                )
        );
        assertThat(exceptionOnMandatoryField).isNotNull();
        exceptionOnMandatoryField = assertThrows(
                MethodArgumentNotValidException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(exceptionOnMandatoryField).isNotNull();
        exceptionOnMandatoryField = assertThrows(
                MethodArgumentNotValidException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .build()
                )
        );
        assertThat(exceptionOnMandatoryField).isNotNull();
    }

    @Test
    public void checkAllMandatoryField() {
        var createNewLocationResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .description("location1 description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(createNewLocationResult.getPayload()).isNotNull();
    }

    @Test
    public void creatingLocationFailWithNotFoundLocationManagerEmail() {
        PersonNotFound locationManagerNotFound = assertThrows(
                PersonNotFound.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .description("location1 description")
                                .locationManagerUserId("bad@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(locationManagerNotFound.getErrorMessage())
                .isNotNull()
                        .contains("bad@slac.stanford.edu");
    }

    @Test
    public void createNewStandaloneLocationAndFindById() {
        var createNewLocationResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .description("location1 description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(createNewLocationResult.getPayload()).isNotNull();

        var fullLocationFound = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        createNewLocationResult.getPayload()
                )
        );
        assertThat(fullLocationFound.getPayload()).isNotNull();
        assertThat(fullLocationFound.getPayload().id()).isEqualTo(createNewLocationResult.getPayload());
    }

    @Test
    public void createNewStandaloneLocationFailsWithBadLocation() {
        var domainNotFound = assertThrows(
                DomainNotFound.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId("bad-id")
                                .name("location1")
                                .description("location1 description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(domainNotFound.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createMultipleStandaloneLocationAndFindAll() {
        String[] idsCreated = new String[10];
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            var result = assertDoesNotThrow(
                    () -> testControllerHelperService.locationControllerCreateNewRoot(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            NewLocationDTO.builder()
                                    .domainId(domainId)
                                    .name("location-%d".formatted((finalI)))
                                    .description("location-%d description".formatted((finalI)))
                                    .locationManagerUserId("user1@slac.stanford.edu")
                                    .build()
                    )
            );
            assertThat(result).isNotNull();
            idsCreated[i] = result.getPayload();
        }

        var allLocationsResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(allLocationsResult.getErrorCode()).isEqualTo(0);
        assertThat(allLocationsResult.getPayload())
                .hasSize(10)
                .extracting(LocationDTO::id)
                .contains(idsCreated);

        allLocationsResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("1"),
                        Optional.empty()
                )
        );
        assertThat(allLocationsResult.getErrorCode()).isEqualTo(0);
        assertThat(allLocationsResult.getPayload())
                .hasSize(1)
                .extracting(LocationDTO::id)
                .contains(idsCreated[1]);
    }


    @Test
    public void failCreateNewStandaloneLocationAndFindByIdWithUnauthorizedUser() {
        var createNewLocationResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .description("location1 description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(createNewLocationResult.getPayload()).isNotNull();

        var notAuthorizeError = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.locationControllerFindById(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        createNewLocationResult.getPayload()
                )
        );
        assertThat(notAuthorizeError.getErrorCode()).isEqualTo(-1);
    }
    @Test
    public void createDependentLocationWithWrongExternalId(){
        var firstLocation = inventoryElementSummaryDTOList.getFirst();
        String externalLocationIdentifier = "bad-domain-id/bad-element-id";
        var errorExternalLocationNotFound = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .externalLocationIdentifier(externalLocationIdentifier)
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(errorExternalLocationNotFound).isNotNull();
        assertThat(errorExternalLocationNotFound.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createDependentLocationAndFetchOK(){
        var firstLocation = inventoryElementSummaryDTOList.getFirst();
        String externalLocationIdentifier = "%s/%s".formatted(firstLocation.getDomainDTO().getId(), firstLocation.getId());
        var createNewLocationResult = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocationDTO.builder()
                                .domainId(domainId)
                                .name("location1")
                                .externalLocationIdentifier(externalLocationIdentifier)
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        assertThat(createNewLocationResult.getPayload()).isNotNull();

        var fullLocationFound = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        createNewLocationResult.getPayload()
                )
        );
        assertThat(fullLocationFound.getPayload()).isNotNull();
        assertThat(fullLocationFound.getPayload().id()).isEqualTo(createNewLocationResult.getPayload());
        assertThat(fullLocationFound.getPayload().name()).isEqualTo(firstLocation.getName());

        var findAllForExternalId = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of(externalLocationIdentifier)
                )
        );
        assertThat(findAllForExternalId.getPayload()).isNotNull();
        assertThat(findAllForExternalId.getPayload())
                .hasSize(1)
                .extracting(LocationDTO::id)
                .contains(createNewLocationResult.getPayload());
    }
}
