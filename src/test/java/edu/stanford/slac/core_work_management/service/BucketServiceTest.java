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
import java.util.ArrayList;
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
                                    .description("bucket-%02d".formatted(finalI))
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
            assertThat(first10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%02d".formatted(i));
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
            assertThat(next10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%02d".formatted(10 + i));
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
            assertThat(previous10Bucket.get(i)).extracting(BucketSlotDTO::description).isEqualTo("bucket-%02d".formatted(i));
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
        assertThat(fullWork.currentBucketAssociation().bucket().id()).isEqualTo(newBucketId);
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

    @Test
    public void findAllThatContainASpecificDate() {
        // List to store created bucket IDs for verification
        List<String> createdBucketIds = new ArrayList<>();

        // Create 100 buckets with incremental 'from' dates
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            LocalDateTime fromDate = LocalDateTime.of(2021, 1, 1, 0, 0).plusMinutes(finalI);
            LocalDateTime toDate = LocalDateTime.of(2021, 1, 3, 23, 0);

            // Create a new bucket
            var newBucketId = assertDoesNotThrow(
                    () -> bucketSlotService.createNew(
                            NewBucketDTO.builder()
                                    .description("bucket-%d".formatted(finalI))
                                    .type(bucketTypeLOVIds.getFirst())
                                    .status(bucketStatusLOVIds.getFirst())
                                    .from(fromDate)
                                    .to(toDate)
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
            createdBucketIds.add(newBucketId);

            // Use the 'fromDate' as the test date to find buckets containing this date
            List<BucketSlotDTO> bucketsContainingDate = bucketSlotService.findAllThatContainsDate(fromDate);

            // Verify that the buckets returned are not empty
            assertThat(bucketsContainingDate).isNotEmpty();

            // Verify that the newly created bucket is among the buckets returned
            boolean bucketFound = bucketsContainingDate.stream()
                    .anyMatch(bucket -> bucket.id().equals(newBucketId));
            assertThat(bucketFound)
                    .withFailMessage("Bucket with ID %s should contain the date %s", newBucketId, fromDate)
                    .isTrue();

            // Verify that all buckets returned actually contain the test date
            for (BucketSlotDTO bucket : bucketsContainingDate) {
                assertThat(bucket.from())
                        .withFailMessage("Bucket 'from' date %s should be before or equal to %s", bucket.from(), fromDate)
                        .isBeforeOrEqualTo(fromDate);
                assertThat(bucket.to())
                        .withFailMessage("Bucket 'to' date %s should be after or equal to %s", bucket.to(), fromDate)
                        .isAfterOrEqualTo(fromDate);
            }
        }

        // Additional test: Use a specific date to find all buckets containing it
        LocalDateTime specificTestDate = LocalDateTime.of(2021, 1, 2, 12, 0);
        List<BucketSlotDTO> bucketsAtSpecificDate = bucketSlotService.findAllThatContainsDate(specificTestDate);

        // Verify that the buckets returned are not empty
        assertThat(bucketsAtSpecificDate)
                .withFailMessage("No buckets found containing the date %s", specificTestDate)
                .isNotEmpty();

        // Verify that all buckets returned contain the specific test date
        for (BucketSlotDTO bucket : bucketsAtSpecificDate) {
            assertThat(bucket.from())
                    .withFailMessage("Bucket 'from' date %s should be before or equal to %s", bucket.from(), specificTestDate)
                    .isBeforeOrEqualTo(specificTestDate);
            assertThat(bucket.to())
                    .withFailMessage("Bucket 'to' date %s should be after or equal to %s", bucket.to(), specificTestDate)
                    .isAfterOrEqualTo(specificTestDate);
        }
    }

    // create test for the bucket start event
    @Test
    public void processBucketStartEvent() {
        // Step 1: Create buckets with specific 'from' and 'to' dates
        int processingTimeoutSeconds = 10;
        LocalDateTime bucket1From = LocalDateTime.of(2021, 1, 1, 8, 0);
        LocalDateTime bucket1To = LocalDateTime.of(2021, 1, 1, 12, 0);

        LocalDateTime bucket2From = LocalDateTime.of(2021, 1, 1, 13, 0);
        LocalDateTime bucket2To = LocalDateTime.of(2021, 1, 1, 17, 0);

        String bucket1Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 1")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket1From)
                        .to(bucket1To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        String bucket2Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 2")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket2From)
                        .to(bucket2To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        assertThat(bucket1Id).isNotNull();
        assertThat(bucket2Id).isNotNull();

        // Step 2: Define 'currentDate' and 'processingTimeoutMinutes'
        LocalDateTime currentDate = LocalDateTime.of(2021, 1, 1, 11, 0);

        // Step 3: Invoke the function under test
        BucketSlotDTO bucketToStart = bucketSlotService.findNextBucketToStart(currentDate, currentDate.plusSeconds(processingTimeoutSeconds));

        // Step 4: Assert the results
        assertThat(bucketToStart).isNotNull();
        assertThat(bucketToStart.id()).isEqualTo(bucket1Id);

        // get wit the same date should return false
        bucketToStart = bucketSlotService.findNextBucketToStart(currentDate, currentDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStart).isNull();

        /// advance to a date that is after the bucket expired for processing
        var currentCheckDate = currentDate.plusSeconds(processingTimeoutSeconds);
        BucketSlotDTO bucketToStartAgain = bucketSlotService.findNextBucketToStart(currentCheckDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStartAgain).isNotNull();

        // now tag as completed
        assertDoesNotThrow(()->bucketSlotService.completeStartEventProcessing(bucket1Id));

        // now it doesn't need to be selected again
        bucketToStartAgain = bucketSlotService.findNextBucketToStart(currentDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStartAgain).isNull();
    }

    @Test
    public void processBucketStopEvent() {
        // Step 1: Create buckets with specific 'from' and 'to' dates
        int processingTimeoutSeconds = 10;
        LocalDateTime bucket1From = LocalDateTime.of(2021, 1, 1, 8, 0);
        LocalDateTime bucket1To = LocalDateTime.of(2021, 1, 1, 12, 0);

        LocalDateTime bucket2From = LocalDateTime.of(2021, 1, 1, 13, 0);
        LocalDateTime bucket2To = LocalDateTime.of(2021, 1, 1, 17, 0);

        String bucket1Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 1")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket1From)
                        .to(bucket1To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        String bucket2Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 2")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket2From)
                        .to(bucket2To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        assertThat(bucket1Id).isNotNull();
        assertThat(bucket2Id).isNotNull();

        // Step 2: Define 'currentDate' and 'processingTimeoutSeconds' for stop event
        LocalDateTime currentDate = LocalDateTime.of(2021, 1, 1, 12, 0);

        // Step 3: Invoke the function under test to find the next bucket to stop
        BucketSlotDTO bucketToStop = bucketSlotService.findNextBucketToStop(currentDate, currentDate.plusSeconds(processingTimeoutSeconds));

        // Step 4: Assert the results
        assertThat(bucketToStop).isNotNull();
        assertThat(bucketToStop.id()).isEqualTo(bucket1Id);

        // Attempt to get the same bucket again should return null
        bucketToStop = bucketSlotService.findNextBucketToStop(currentDate, currentDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStop).isNull();

        // Advance to a date after the processing timeout
        var currentCheckDate = currentDate.plusSeconds(processingTimeoutSeconds);
        BucketSlotDTO bucketToStopAgain = bucketSlotService.findNextBucketToStop(currentCheckDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStopAgain).isNotNull();

        // Now mark the stop event as completed
        assertDoesNotThrow(() -> bucketSlotService.completeStopEventProcessing(bucket1Id));

        // The bucket should not be selected again after completion
        bucketToStopAgain = bucketSlotService.findNextBucketToStop(currentDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStopAgain).isNull();
    }

    @Test
    public void processBucketStartAndStopEvents() {
        // Step 1: Create buckets with specific 'from' and 'to' dates
        int processingTimeoutSeconds = 10;
        LocalDateTime bucket1From = LocalDateTime.of(2021, 1, 1, 8, 0);
        LocalDateTime bucket1To = LocalDateTime.of(2021, 1, 1, 12, 0);

        LocalDateTime bucket2From = LocalDateTime.of(2021, 1, 1, 13, 0);
        LocalDateTime bucket2To = LocalDateTime.of(2021, 1, 1, 17, 0);

        String bucket1Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 1")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket1From)
                        .to(bucket1To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        String bucket2Id = bucketSlotService.createNew(
                NewBucketDTO.builder()
                        .description("Bucket 2")
                        .type(bucketTypeLOVIds.getFirst())
                        .status(bucketStatusLOVIds.getFirst())
                        .from(bucket2From)
                        .to(bucket2To)
                        .domainIds(Set.of(domainDTO.id()))
                        .admittedWorkTypeIds(Set.of(
                                BucketSlotWorkTypeDTO.builder()
                                        .domainId(domainDTO.id())
                                        .workTypeId(newWorkTypeId)
                                        .build()
                        ))
                        .build()
        );

        assertThat(bucket1Id).isNotNull();
        assertThat(bucket2Id).isNotNull();

        // Step 2: Process Start Event for Bucket 1
        LocalDateTime startEventDate = LocalDateTime.of(2021, 1, 1, 7, 59);
        BucketSlotDTO bucketToStart = bucketSlotService.findNextBucketToStart(startEventDate, startEventDate.plusSeconds(processingTimeoutSeconds));

        // Assert that no bucket is ready to start yet
        assertThat(bucketToStart).isNull();

        // Move to time when bucket 1 should start
        startEventDate = LocalDateTime.of(2021, 1, 1, 8, 0);
        bucketToStart = bucketSlotService.findNextBucketToStart(startEventDate, startEventDate.plusSeconds(processingTimeoutSeconds));

        // Assert that bucket 1 is ready to start
        assertThat(bucketToStart).isNotNull();
        assertThat(bucketToStart.id()).isEqualTo(bucket1Id);

        // Attempt to start the same bucket again should return null
        BucketSlotDTO duplicateStartAttempt = bucketSlotService.findNextBucketToStart(startEventDate, startEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(duplicateStartAttempt).isNull();

        // Simulate processing timeout by moving time forward
        LocalDateTime processingExpiredDate = startEventDate.plusSeconds(processingTimeoutSeconds);
        BucketSlotDTO bucketToStartAgain = bucketSlotService.findNextBucketToStart(processingExpiredDate, processingExpiredDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStartAgain).isNotNull();
        assertThat(bucketToStartAgain.id()).isEqualTo(bucket1Id);

        // Mark the start event as completed
        assertDoesNotThrow(() -> bucketSlotService.completeStartEventProcessing(bucket1Id));

        // Attempt to start again after completion should return null
        BucketSlotDTO afterCompletionStartAttempt = bucketSlotService.findNextBucketToStart(startEventDate, processingExpiredDate.plusSeconds(processingTimeoutSeconds));
        assertThat(afterCompletionStartAttempt).isNull();

        // Step 3: Process Stop Event for Bucket 1
        LocalDateTime stopEventDate = bucket1To; // The 'to' date when the bucket is supposed to stop

        // Find the bucket to stop
        BucketSlotDTO bucketToStop = bucketSlotService.findNextBucketToStop(stopEventDate, stopEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStop).isNotNull();
        assertThat(bucketToStop.id()).isEqualTo(bucket1Id);

        // Attempt to stop the same bucket again should return null
        BucketSlotDTO duplicateStopAttempt = bucketSlotService.findNextBucketToStop(stopEventDate, stopEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(duplicateStopAttempt).isNull();

        // Simulate processing timeout by moving time forward
        LocalDateTime stopProcessingExpiredDate = stopEventDate.plusSeconds(processingTimeoutSeconds);
        BucketSlotDTO bucketToStopAgain = bucketSlotService.findNextBucketToStop(stopProcessingExpiredDate, stopProcessingExpiredDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStopAgain).isNotNull();
        assertThat(bucketToStopAgain.id()).isEqualTo(bucket1Id);

        // Mark the stop event as completed
        assertDoesNotThrow(() -> bucketSlotService.completeStopEventProcessing(bucket1Id));

        // Attempt to stop again after completion should return null
        BucketSlotDTO afterCompletionStopAttempt = bucketSlotService.findNextBucketToStop(stopEventDate, stopProcessingExpiredDate.plusSeconds(processingTimeoutSeconds));
        assertThat(afterCompletionStopAttempt).isNull();

        // Step 4: Process Start Event for Bucket 2
        LocalDateTime bucket2StartEventDate = bucket2From;
        BucketSlotDTO bucket2ToStart = bucketSlotService.findNextBucketToStart(bucket2StartEventDate, bucket2StartEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucket2ToStart).isNotNull();
        assertThat(bucket2ToStart.id()).isEqualTo(bucket2Id);

        // Mark the start event as completed for Bucket 2
        assertDoesNotThrow(() -> bucketSlotService.completeStartEventProcessing(bucket2Id));

        // Step 5: Process Stop Event for Bucket 2
        LocalDateTime bucket2StopEventDate = bucket2To;
        BucketSlotDTO bucket2ToStop = bucketSlotService.findNextBucketToStop(bucket2StopEventDate, bucket2StopEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucket2ToStop).isNotNull();
        assertThat(bucket2ToStop.id()).isEqualTo(bucket2Id);

        // Mark the stop event as completed for Bucket 2
        assertDoesNotThrow(() -> bucketSlotService.completeStopEventProcessing(bucket2Id));

        // Final Assertions: Ensure no buckets are pending for start or stop
        BucketSlotDTO noBucketToStart = bucketSlotService.findNextBucketToStart(LocalDateTime.now(), LocalDateTime.now().plusSeconds(processingTimeoutSeconds));
        assertThat(noBucketToStart).isNull();

        BucketSlotDTO noBucketToStop = bucketSlotService.findNextBucketToStop(LocalDateTime.now(), LocalDateTime.now().plusSeconds(processingTimeoutSeconds));
        assertThat(noBucketToStop).isNull();
    }
}
