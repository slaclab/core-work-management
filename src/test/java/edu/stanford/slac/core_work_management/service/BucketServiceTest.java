package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.migration.M1003_InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.*;
import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.AssertionsForClassTypes;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "async-ops"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BucketServiceTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private BucketService bucketSlotService;
    @Autowired
    private WorkService workService;
    @Autowired
    private ShopGroupService shopGroupService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private MongoTemplate mongoTemplate;
    private List<String> bucketTypeLOVIds = null;
    private List<String> bucketStatusLOVIds = null;
    private DomainDTO domainDTO;
    private String newWorkTypeId;
    private String shopGroupId;
    private String locationId;

    @BeforeAll
    public void initLOV() {
        mongoTemplate.remove(new Query(), LOVElement.class);
        M1003_InitBucketTypeLOV initBucketTypeLOV = new M1003_InitBucketTypeLOV(lovService);
        assertDoesNotThrow(initBucketTypeLOV::changeSet);

        bucketTypeLOVIds = lovService.findAllByGroupName("BucketType").stream().map(LOVElementDTO::id).toList();
        bucketStatusLOVIds = lovService.findAllByGroupName("BucketStatus").stream().map(LOVElementDTO::id).toList();
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), BucketSlot.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), Work.class);

        domainDTO = domainService.createNewAndGet(
                NewDomainDTO.builder()
                        .name("TEST domain")
                        .description("Test domain description")
                        .workflowImplementations(
                                Set.of(
                                        "DummyParentWorkflow"
                                )
                        )
                        .build()
        );
        assertThat(domainDTO).isNotNull();

        newWorkTypeId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domainDTO.id(),
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(domainDTO.workflows().stream().findFirst().get().id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        shopGroupId =
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
                );
        AssertionsForClassTypes.assertThat(shopGroupId).isNotEmpty();

        locationId = assertDoesNotThrow(
                () -> locationService.createNew(
                        domainDTO.id(),
                        NewLocationDTO.builder()
                                .name("SLAC")
                                .description("SLAC National Accelerator Laboratory")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();
    }

    @Test
    public void createNewBucketAndFindIt() {
        var newBucketId = assertDoesNotThrow(
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                .domainIds(Set.of(domainDTO.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO.builder()
                                                        .domainId(domainDTO.id())
                                                        .workTypeId(newWorkTypeId)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newBucketId).isNotNull();

        var fullBucketFound = assertDoesNotThrow(
                () -> bucketSlotService.findById(newBucketId)
        );
        assertThat(fullBucketFound).isNotNull();
        assertThat(fullBucketFound.id()).isEqualTo(newBucketId);
        assertThat(fullBucketFound.description()).isEqualTo("bucket-1");
        assertThat(fullBucketFound.type().id()).isEqualTo(bucketTypeLOVIds.get(0));
        assertThat(fullBucketFound.status().id()).isEqualTo(bucketStatusLOVIds.get(0));
        assertThat(fullBucketFound.from()).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0));
        assertThat(fullBucketFound.to()).isEqualTo(LocalDateTime.of(2021, 1, 3, 23, 0));
        assertThat(fullBucketFound.admittedWorkType()).hasSize(1);
        assertThat(fullBucketFound.admittedWorkType().iterator().next().id()).isEqualTo(newWorkTypeId);
        assertThat(fullBucketFound.admittedWorkType().iterator().next().domainId()).isEqualTo(domainDTO.id());
    }

    @Test
    public void updateBucketAndFindIt() {
        var newBucketId = assertDoesNotThrow(
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                .domainIds(Set.of(domainDTO.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO.builder()
                                                        .domainId(domainDTO.id())
                                                        .workTypeId(newWorkTypeId)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newBucketId).isNotNull();

        // update bucket
        assertDoesNotThrow(
                () -> bucketSlotService.update(
                        newBucketId,
                        UpdateBucketDTO.builder()
                                .description("bucket-1 updated")
                                .type(bucketTypeLOVIds.get(1))
                                .from(LocalDateTime.of(2022, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2022, 1, 3, 23, 0))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO.builder()
                                                        .domainId(domainDTO.id())
                                                        .workTypeId(newWorkTypeId)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );


        var fullBucketFound = assertDoesNotThrow(
                () -> bucketSlotService.findById(newBucketId)
        );
        assertThat(fullBucketFound).isNotNull();
        assertThat(fullBucketFound.id()).isEqualTo(newBucketId);
        assertThat(fullBucketFound.description()).isEqualTo("bucket-1 updated");
        assertThat(fullBucketFound.type().id()).isEqualTo(bucketTypeLOVIds.get(1));
        assertThat(fullBucketFound.status().id()).isEqualTo(bucketStatusLOVIds.get(0));
        assertThat(fullBucketFound.from()).isEqualTo(LocalDateTime.of(2022, 1, 1, 0, 0));
        assertThat(fullBucketFound.to()).isEqualTo(LocalDateTime.of(2022, 1, 3, 23, 0));
        assertThat(fullBucketFound.admittedWorkType()).hasSize(1);
        assertThat(fullBucketFound.admittedWorkType().iterator().next().id()).isEqualTo(newWorkTypeId);
        assertThat(fullBucketFound.admittedWorkType().iterator().next().domainId()).isEqualTo(domainDTO.id());

    }

    @Test
    public void testFieldReferenceToFindLOV() {
        var allPossibleBucketType = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, null, "bucket", "type")
        );
        assertThat(allPossibleBucketType).isNotEmpty();
        assertThat(allPossibleBucketType).allMatch(lov -> bucketTypeLOVIds.contains(lov.id()));

        var allPossibleBucketStatus = assertDoesNotThrow(
                () -> lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, null, "bucket", "status")
        );
        assertThat(allPossibleBucketStatus).isNotEmpty();
        assertThat(allPossibleBucketStatus).allMatch(lov -> bucketStatusLOVIds.contains(lov.id()));
    }

    @Test
    public void failedCreateBucketWithWrongData() {
        var failed1 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .build()
                )
        );
        assertThat(failed1).isNotNull();
        assertThat(failed1.getConstraintViolations()).hasSize(7);
        var failed2 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed2).isNotNull();
        assertThat(failed2.getConstraintViolations()).hasSize(6);

        var failed3 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed3).isNotNull();
        assertThat(failed3.getConstraintViolations()).hasSize(5);

        var failed4 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed4).isNotNull();
        assertThat(failed4.getConstraintViolations()).hasSize(4);

        var failed5 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed5).isNotNull();
        assertThat(failed5.getConstraintViolations()).hasSize(3);

        var failed6 = assertThrows(
                ConstraintViolationException.class,
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed6).isNotNull();
        assertThat(failed6.getConstraintViolations()).hasSize(3);
    }

    @Test
    public void findAll() {
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            var newBucketId = assertDoesNotThrow(
                    () -> bucketSlotService.createNew(
                            NewBucketDTO.builder()
                                    .description("bucket-%d".formatted(finalI))
                                    .type(bucketTypeLOVIds.getFirst())
                                    .status(bucketStatusLOVIds.getFirst())
                                    .from(LocalDateTime.of(2021, 1, 1, 0, 0).plus(finalI, ChronoUnit.MINUTES))
                                    .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                    .domainIds(Set.of(domainDTO.id()))
                                    .admittedWorkTypeIds(
                                            Set.of(
                                                    BucketSlotWorkTypeDTO.builder()
                                                            .domainId(domainDTO.id())
                                                            .workTypeId(newWorkTypeId)
                                                            .build()
                                            )
                                    )
                                    .build()
                    )
            );
            assertThat(newBucketId).isNotNull();
        }

        var first10Bucket = assertDoesNotThrow(
                () -> bucketSlotService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        // check if i received the last element in decreasing order
        assertThat(first10Bucket).isNotNull();
        assertThat(first10Bucket).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(first10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%d".formatted(99 - i));
        }

        // find next page
        var next10Bucket = assertDoesNotThrow(
                () -> bucketSlotService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .limit(10)
                                .anchorID(first10Bucket.getLast().id())
                                .build()
                )
        );
        // check next page
        assertThat(next10Bucket).isNotNull();
        assertThat(next10Bucket).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(next10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%d".formatted(89 - i));
        }

        // check previous page
        var previous10Bucket = assertDoesNotThrow(
                () -> bucketSlotService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .contextSize(10)
                                .anchorID(next10Bucket.getFirst().id())
                                .build()
                )
        );
        assertThat(previous10Bucket).isNotNull();
        assertThat(previous10Bucket).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(previous10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%d".formatted(98 - i));
        }
    }

    @Test
    public void testWorkAssociationToABucket() {
        var newBucketId = assertDoesNotThrow(
                () -> bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                .domainIds(Set.of(domainDTO.id()))
                                .admittedWorkTypeIds(
                                        Set.of(
                                                BucketSlotWorkTypeDTO.builder()
                                                        .domainId(domainDTO.id())
                                                        .workTypeId(newWorkTypeId)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newBucketId).isNotNull();

        // create a new work
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainDTO.id(),
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        // now try to add to the bucket
        assertDoesNotThrow(
                () -> workService.associateWorkToBucketSlot(
                        domainDTO.id(),
                        newWorkId,
                        newBucketId,
                        Optional.empty()
                )
        );

        // get full work
        var fullWork = assertDoesNotThrow(
                () -> workService.findWorkById(domainDTO.id(), newWorkId, WorkDetailsOptionDTO.builder().build())
        );
        assertThat(fullWork).isNotNull();
        assertThat(fullWork.currentBucketAssociation()).isNotNull();
        assertThat(fullWork.currentBucketAssociation().bucketId()).isEqualTo(newBucketId);
        assertThat(fullWork.currentBucketAssociation().rolled()).isEqualTo(false);

        // try to add to the same bucket should trow an exception
        assertThrows(
                ControllerLogicException.class,
                () -> workService.associateWorkToBucketSlot(
                        domainDTO.id(),
                        newWorkId,
                        newBucketId,
                        Optional.empty()
                )
        );

        // try to remove the work from the bucket
        assertDoesNotThrow(
                () -> workService.removeWorkFromBucketSlot(
                        domainDTO.id(),
                        newWorkId,
                        newBucketId
                )
        );

        // now try to add to the bucket
        assertDoesNotThrow(
                () -> workService.associateWorkToBucketSlot(
                        domainDTO.id(),
                        newWorkId,
                        newBucketId,
                        Optional.empty()
                )
        );
    }
}
