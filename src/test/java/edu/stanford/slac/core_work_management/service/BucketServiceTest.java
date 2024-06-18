package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.migration.InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.LOVElement;
import jakarta.validation.ConstraintViolationException;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test",  "async-ops"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BucketServiceTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private BucketService bucketSlotService;
    @Autowired
    private MongoTemplate mongoTemplate;
    private List<String> bucketTypeLOVIds = null;
    private List<String> bucketStatusLOVIds = null;

    @BeforeAll
    public void initLOV() {
        mongoTemplate.remove(new Query(), LOVElement.class);
        InitBucketTypeLOV initBucketTypeLOV = new InitBucketTypeLOV(lovService);
        assertDoesNotThrow(()->initBucketTypeLOV.changeSet());

        bucketTypeLOVIds = lovService.findAllByGroupName("BucketType").stream().map(LOVElementDTO::id).toList();
        bucketStatusLOVIds = lovService.findAllByGroupName("BucketStatus").stream().map(LOVElementDTO::id).toList();
    }

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), BucketSlot.class);
    }

    @Test
    public void createNewBucketAndFindIt() {
        var newBucketId = assertDoesNotThrow(
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                .build()
                )
        );
        assertThat(newBucketId).isNotNull();

        var fullBucketFound = assertDoesNotThrow(
                ()->bucketSlotService.findById(newBucketId)
        );
        assertThat(fullBucketFound).isNotNull();
        assertThat(fullBucketFound.id()).isEqualTo(newBucketId);
        assertThat(fullBucketFound.description()).isEqualTo("bucket-1");
        assertThat(fullBucketFound.type().id()).isEqualTo(bucketTypeLOVIds.get(0));
        assertThat(fullBucketFound.status().id()).isEqualTo(bucketStatusLOVIds.get(0));
        assertThat(fullBucketFound.from()).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0));
        assertThat(fullBucketFound.to()).isEqualTo(LocalDateTime.of(2021, 1, 3, 23, 0));
    }

    @Test
    public void testFieldReferenceToFindLOV(){
        var allPossibleBucketType = assertDoesNotThrow(
                ()->lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, "bucket", "type")
        );
        assertThat(allPossibleBucketType).isNotEmpty();
        assertThat(allPossibleBucketType).allMatch(lov->bucketTypeLOVIds.contains(lov.id()));

        var allPossibleBucketStatus = assertDoesNotThrow(
                ()->lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, "bucket", "status")
        );
        assertThat(allPossibleBucketStatus).isNotEmpty();
        assertThat(allPossibleBucketStatus).allMatch(lov->bucketStatusLOVIds.contains(lov.id()));
    }

    @Test
    public void failedCreateBucketWithWrongData() {
        var failed1 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .build()
                )
        );
        assertThat(failed1).isNotNull();
        assertThat(failed1.getConstraintViolations()).hasSize(5);
        var failed2 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed2).isNotNull();
        assertThat(failed2.getConstraintViolations()).hasSize(4);

        var failed3 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed3).isNotNull();
        assertThat(failed3.getConstraintViolations()).hasSize(3);

        var failed4 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed4).isNotNull();
        assertThat(failed4.getConstraintViolations()).hasSize(2);

        var failed5 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .type(bucketTypeLOVIds.get(0))
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed5).isNotNull();
        assertThat(failed5.getConstraintViolations()).hasSize(1);

        var failed6 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketDTO.builder()
                                .description("bucket-1")
                                .status(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed6).isNotNull();
        assertThat(failed6.getConstraintViolations()).hasSize(1);
    }

    @Test
    public void findAll() {
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            var newBucketId = assertDoesNotThrow(
                    ()->bucketSlotService.createNew(
                            NewBucketDTO.builder()
                                    .description("bucket-%d".formatted(finalI))
                                    .type(bucketTypeLOVIds.get(0))
                                    .status(bucketStatusLOVIds.get(0))
                                    .from(LocalDateTime.of(2021, 1, 1, 0, 0).plus(finalI, ChronoUnit.MINUTES))
                                    .to(LocalDateTime.of(2021, 1, 3, 23, 0))
                                    .build()
                    )
            );
            assertThat(newBucketId).isNotNull();
        }

        var first10Bucket = assertDoesNotThrow(
                ()->bucketSlotService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        // check if i received the last element in decreasing order
        assertThat(first10Bucket).isNotNull();
        assertThat(first10Bucket).hasSize(10);
        for(int i = 0; i < 10; i++){
            assertThat(first10Bucket.get(i)).extracting(BucketDTO::description).isEqualTo("bucket-%d".formatted(99-i));
        }

        // find next page
        var next10Bucket = assertDoesNotThrow(
                ()->bucketSlotService.findAll(
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
        for(int i = 0; i < 10; i++){
            assertThat(next10Bucket.get(i)).extracting(BucketDTO::description).isEqualTo("bucket-%d".formatted(89-i));
        }

        // check previous page
        var previous10Bucket = assertDoesNotThrow(
                ()->bucketSlotService.findAll(
                        BucketQueryParameterDTO
                                .builder()
                                .contextSize(10)
                                .anchorID(next10Bucket.getFirst().id())
                                .build()
                )
        );
        assertThat(previous10Bucket).isNotNull();
        assertThat(previous10Bucket).hasSize(10);
        for(int i = 0; i < 10; i++){
            assertThat(previous10Bucket.get(i)).extracting(BucketDTO::description).isEqualTo("bucket-%d".formatted(98-i));
        }
    }
}
