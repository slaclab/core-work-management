package edu.stanford.slac.core_work_management.controller.domain.tec;

import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserInputDTO;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import edu.stanford.slac.core_work_management.controller.domain.BaseWorkflowDomainTest;
import edu.stanford.slac.core_work_management.controller.domain.DomainTestInfo;
import edu.stanford.slac.core_work_management.migration.M1000_InitLOV;
import edu.stanford.slac.core_work_management.migration.M1001_InitTECDomain;
import edu.stanford.slac.core_work_management.migration.M1003_InitBucketTypeLOV;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.HelperService;
import edu.stanford.slac.core_work_management.service.LOVService;
import org.apache.kafka.clients.admin.AdminClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class TECDomainEnvironmentTest extends BaseWorkflowDomainTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private LOVService lovService;
    @Autowired
    private CWMAppProperties cwmAppProperties;
    @Autowired
    private KafkaAdmin kafkaAdmin;

    public TECDomainEnvironmentTest(HelperService helperService) {
        super(helperService);
    }

    public DomainTestInfo init() {
        DomainTestInfo domainTestInfo = new DomainTestInfo();
        mongoTemplate.remove(Domain.class).all();
        mongoTemplate.remove(WorkType.class).all();
        mongoTemplate.remove(LOVElement.class).all();
        mongoTemplate.remove(Location.class).all();
        mongoTemplate.remove(ShopGroup.class).all();

        // init general lov
        M1000_InitLOV initLOV = new M1000_InitLOV(lovService);
        assertDoesNotThrow(initLOV::initLOV);

        // init bucket type lov
        M1003_InitBucketTypeLOV initBucketTypeLOV = new M1003_InitBucketTypeLOV(lovService);
        assertDoesNotThrow(initBucketTypeLOV::changeSet);

        // retrieve all lov elements
        domainTestInfo.lovElementBucketType = lovService.findAllByGroupName("BucketType");
        domainTestInfo.lovElementBucketStatus = lovService.findAllByGroupName("BucketStatus");

        // init tec domain
        M1001_InitTECDomain initWorkType = new M1001_InitTECDomain(lovService, domainService);
        DomainDTO tecDomain = assertDoesNotThrow(initWorkType::initTECDomain);
        AssertionsForClassTypes.assertThat(tecDomain).isNotNull();
        domainTestInfo.domain = tecDomain;

        // find all work type
        var workTypesResult = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerFindAllWorkTypes(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id()
                )
        );
        AssertionsForClassTypes.assertThat(workTypesResult).isNotNull();
        AssertionsForClassTypes.assertThat(workTypesResult.getErrorCode()).isEqualTo(0);
        assertThat(workTypesResult.getPayload()).isNotNull().isNotEmpty().hasSize(2);
        domainTestInfo.workTypes = workTypesResult.getPayload();

        // create locations
        var location10Result = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewLocationDTO.builder()
                                .name("Location10")
                                .description("LocationA description")
                                .locationManagerUserId("user10@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(location10Result).isNotNull();
        AssertionsForClassTypes.assertThat(location10Result.getErrorCode()).isEqualTo(0);
        domainTestInfo.locationsIDs.add(
                assertDoesNotThrow(
                        () -> testControllerHelperService.locationControllerFindById(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                tecDomain.id(),
                                location10Result.getPayload()
                        )
                ).getPayload()
        );

        var location20Result = assertDoesNotThrow(
                () -> testControllerHelperService.locationControllerCreateNewRoot(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewLocationDTO.builder()
                                .name("Location20")
                                .description("LocationA description")
                                .locationManagerUserId("user20@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(location20Result).isNotNull();
        AssertionsForClassTypes.assertThat(location20Result.getErrorCode()).isEqualTo(0);
        // fetch the full location
        domainTestInfo.locationsIDs.add(
                assertDoesNotThrow(
                        () -> testControllerHelperService.locationControllerFindById(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                tecDomain.id(),
                                location20Result.getPayload()
                        )
                ).getPayload());


        // add shop group
        var shopGroup15Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewShopGroupDTO.builder()
                                .name("shop15")
                                .description("shop15 description")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user15@slac.stanford.edu")
                                                        .isLeader(true)
                                                        .build(),
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user16@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(shopGroup15Result).isNotNull();
        AssertionsForClassTypes.assertThat(shopGroup15Result.getErrorCode()).isEqualTo(0);
        domainTestInfo.shopGroups.add(
                assertDoesNotThrow(
                        () -> testControllerHelperService.shopGroupControllerFindById(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                tecDomain.id(),
                                shopGroup15Result.getPayload()
                        )
                ).getPayload()
        );

        // create shop group 17
        var shopGroup17Result = assertDoesNotThrow(
                () -> testControllerHelperService.shopGroupControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewShopGroupDTO.builder()
                                .name("shop17")
                                .description("shop17 description")
                                .users(
                                        of(
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user17@slac.stanford.edu")
                                                        .isLeader(true)
                                                        .build(),
                                                ShopGroupUserInputDTO.builder()
                                                        .userId("user18@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(shopGroup17Result).isNotNull();
        AssertionsForClassTypes.assertThat(shopGroup17Result.getErrorCode()).isEqualTo(0);
        domainTestInfo.shopGroups.add(
                assertDoesNotThrow(
                        () -> testControllerHelperService.shopGroupControllerFindById(
                                mockMvc,
                                status().isOk(),
                                Optional.of("user1@slac.stanford.edu"),
                                tecDomain.id(),
                                shopGroup17Result.getPayload()
                        )
                ).getPayload()
        );
        return domainTestInfo;
    }

    public void clean(DomainTestInfo domainTestInfo) {
        // clean kafka queue for test
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            List<String> topicsToDelete = List.of(
                    cwmAppProperties.getWorkflowProcessingTopic(),
                    String.format("%s-retry-2000", cwmAppProperties.getWorkflowProcessingTopic()),
                    String.format("%s-retry-4000", cwmAppProperties.getWorkflowProcessingTopic())
            );

            // Delete topics that actually exist
            topicsToDelete.stream()
                    .filter(existingTopics::contains)
                    .forEach(topic -> {
                        try {
                            adminClient.deleteTopics(Collections.singletonList(topic)).all().get();
                        } catch (Exception e) {
                            System.err.println("Failed to delete topic " + topic + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to recreate Kafka topic", e);
        }
    }
}
