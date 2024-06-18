package edu.stanford.slac.core_work_management.controller;

import edu.stanford.slac.core_work_management.api.v1.dto.LOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketDTO;
import edu.stanford.slac.core_work_management.migration.InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.service.BucketService;
import edu.stanford.slac.core_work_management.service.LOVService;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MaintenanceControllerTest {
    @Autowired
    TestControllerHelperService testControllerHelperService;
    @Autowired
    private LOVService lovService;
    @Autowired
    private BucketService bucketSlotService;
    @Autowired
    private MongoTemplate mongoTemplate;
    private List<String> bucketTypeLOVIds = null;
    private List<String> bucketStatusLOVIds = null;
    @Autowired
    private MockMvc mockMvc;

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
    public void crateBucketSlot() {
        var createNewBucketResult = assertDoesNotThrow(
                ()-> testControllerHelperService.maintenanceControllerCreateNewBucket(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewBucketDTO
                                .builder()
                                .description("test")
                                .bucketType(bucketTypeLOVIds.get(0))
                                .bucketStatus(bucketStatusLOVIds.get(0))
                                .from(LocalDateTime.of(2021, 1, 1, 0, 0))
                                .to(LocalDateTime.of(2021, 1, 2, 23, 0))
                                .build()
                )
        );
        assertThat(createNewBucketResult).isNotNull();
        assertThat(createNewBucketResult.getPayload()).isNotNull();

        // get the full stored bucket
        var fullBucketResult = assertDoesNotThrow(
                ()-> testControllerHelperService.maintenanceControllerFindBucketById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        createNewBucketResult.getPayload()
                )
        );
        assertThat(fullBucketResult).isNotNull();
        assertThat(fullBucketResult.getPayload()).isNotNull();
        assertThat(fullBucketResult.getPayload().id()).isEqualTo(createNewBucketResult.getPayload());
        assertThat(fullBucketResult.getPayload().description()).isEqualTo("test");
        assertThat(fullBucketResult.getPayload().bucketType().id()).isEqualTo(bucketTypeLOVIds.get(0));
        assertThat(fullBucketResult.getPayload().bucketStatus().id()).isEqualTo(bucketStatusLOVIds.get(0));
        assertThat(fullBucketResult.getPayload().from()).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0));
        assertThat(fullBucketResult.getPayload().to()).isEqualTo(LocalDateTime.of(2021, 1, 2, 23, 0));
    }

    @Test
    public void findAllTest() {
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            var createNewBucketResult = assertDoesNotThrow(
                    () -> testControllerHelperService.maintenanceControllerCreateNewBucket(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            NewBucketDTO
                                    .builder()
                                    .description("test-%d".formatted(finalI))
                                    .bucketType(bucketTypeLOVIds.get(0))
                                    .bucketStatus(bucketStatusLOVIds.get(0))
                                    .from(LocalDateTime.of(2021, 1, 1, 0, 0).plus(finalI, ChronoUnit.MINUTES))
                                    .to(LocalDateTime.of(2021, 1, 2, 23, 0))
                                    .build()
                    )
            );
            assertThat(createNewBucketResult).isNotNull();
            assertThat(createNewBucketResult.getPayload()).isNotNull();
        }

        // find first page
        var firstPageResult = assertDoesNotThrow(
                ()-> testControllerHelperService.maintenanceControllerFindAllBuckets(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(firstPageResult).isNotNull();
        assertThat(firstPageResult.getPayload()).isNotNull();
        assertThat(firstPageResult.getPayload().size()).isEqualTo(10);
        for(int i = 0; i < 10; i++) {
            assertThat(firstPageResult.getPayload().get(i).description()).isEqualTo("test-%d".formatted(99-i));
        }
    }
}
