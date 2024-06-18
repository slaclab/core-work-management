package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketSlotDTO;
import edu.stanford.slac.core_work_management.migration.InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.LOVElement;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
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
public class BucketSlotServiceTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private BucketSlotService bucketSlotService;
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
                        NewBucketSlotDTO.builder()
                                .description("bucket-1")
                                .bucketType(bucketTypeLOVIds.get(0))
                                .bucketStatus(bucketStatusLOVIds.get(0))
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
        assertThat(fullBucketFound.bucketType().id()).isEqualTo(bucketTypeLOVIds.get(0));
        assertThat(fullBucketFound.bucketStatus().id()).isEqualTo(bucketStatusLOVIds.get(0));
        assertThat(fullBucketFound.from()).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0));
        assertThat(fullBucketFound.to()).isEqualTo(LocalDateTime.of(2021, 1, 3, 23, 0));
    }

    @Test
    public void testFieldReferenceToFindLOV(){
        var allPossibleBucketType = assertDoesNotThrow(
                ()->lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, "bucket", "bucketType")
        );
        assertThat(allPossibleBucketType).isNotEmpty();
        assertThat(allPossibleBucketType).allMatch(lov->bucketTypeLOVIds.contains(lov.id()));

        var allPossibleBucketStatus = assertDoesNotThrow(
                ()->lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Bucket, "bucket", "bucketStatus")
        );
        assertThat(allPossibleBucketStatus).isNotEmpty();
        assertThat(allPossibleBucketStatus).allMatch(lov->bucketStatusLOVIds.contains(lov.id()));
    }

    @Test
    public void failedCreateBucketWithWrongData() {
        var failed1 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketSlotDTO.builder()
                                .build()
                )
        );
        assertThat(failed1).isNotNull();
        assertThat(failed1.getConstraintViolations()).hasSize(5);
        var failed2 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketSlotDTO.builder()
                                .bucketType(bucketTypeLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed2).isNotNull();
        assertThat(failed2.getConstraintViolations()).hasSize(4);

        var failed3 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketSlotDTO.builder()
                                .bucketType(bucketTypeLOVIds.get(0))
                                .bucketStatus(bucketStatusLOVIds.get(0))
                                .build()
                )
        );
        assertThat(failed3).isNotNull();
        assertThat(failed3.getConstraintViolations()).hasSize(3);

        var failed4 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketSlotDTO.builder()
                                .bucketType(bucketTypeLOVIds.get(0))
                                .bucketStatus(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed4).isNotNull();
        assertThat(failed4.getConstraintViolations()).hasSize(2);

        var failed5 = assertThrows(
                ConstraintViolationException.class,
                ()->bucketSlotService.createNew(
                        NewBucketSlotDTO.builder()
                                .bucketType(bucketTypeLOVIds.get(0))
                                .bucketStatus(bucketStatusLOVIds.get(0))
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
                        NewBucketSlotDTO.builder()
                                .description("bucket-1")
                                .bucketStatus(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .build()
                )
        );
        assertThat(failed6).isNotNull();
        assertThat(failed6.getConstraintViolations()).hasSize(1);
    }
}
