package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketSlotDTO;
import edu.stanford.slac.core_work_management.migration.InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.LOVElement;
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
    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), LOVElement.class);
        mongoTemplate.remove(new Query(), BucketSlot.class);

        InitBucketTypeLOV initBucketTypeLOV = new InitBucketTypeLOV(lovService);
        assertDoesNotThrow(()->initBucketTypeLOV.changeSet());

        bucketTypeLOVIds = lovService.findAllByGroupName("BucketType").stream().map(LOVElementDTO::id).toList();
        bucketStatusLOVIds = lovService.findAllByGroupName("BucketStatus").stream().map(LOVElementDTO::id).toList();
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
}
