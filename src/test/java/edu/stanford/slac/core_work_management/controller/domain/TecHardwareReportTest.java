package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import edu.stanford.slac.core_work_management.migration.M1000_InitLOV;
import edu.stanford.slac.core_work_management.migration.M1001_InitTECDomain;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.*;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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
public class TecHardwareReportTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LOVService lovService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    // test tec domain
    private DomainDTO tecDomain = null;
    private List<WorkTypeDTO> workTypes = null;
    private String location10 = null;
    private String location20 = null;
    private String shopGroup15 = null;
    private String shopGroup17 = null;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(Domain.class).all();
        mongoTemplate.remove(WorkType.class).all();
        mongoTemplate.remove(LOVElement.class).all();
        mongoTemplate.remove(Location.class).all();
        mongoTemplate.remove(ShopGroup.class).all();

        M1000_InitLOV initLOV = new M1000_InitLOV(lovService);
        assertDoesNotThrow(initLOV::initLOV);

        M1001_InitTECDomain initWorkType = new M1001_InitTECDomain(lovService, domainService);
        tecDomain = assertDoesNotThrow(initWorkType::initTECDomain);
        assertThat(tecDomain).isNotNull();
        // find all work type
        var workTypesResult = assertDoesNotThrow(
                ()->testControllerHelperService.domainControllerFindAllWorkTypes(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id()
                )
        );
        assertThat(workTypesResult).isNotNull();
        assertThat(workTypesResult.getErrorCode()).isEqualTo(0);
        workTypes = workTypesResult.getPayload();
        assertThat(workTypes).isNotNull().isNotEmpty();

        // create locations
        var location10Result = assertDoesNotThrow(
                ()->testControllerHelperService.locationControllerCreateNewRoot(
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
        assertThat(location10Result).isNotNull();
        assertThat(location10Result.getErrorCode()).isEqualTo(0);
        location10 = location10Result.getPayload();

        var location20Result = assertDoesNotThrow(
                ()->testControllerHelperService.locationControllerCreateNewRoot(
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
        assertThat(location20Result).isNotNull();
        assertThat(location20Result.getErrorCode()).isEqualTo(0);
        location20 = location20Result.getPayload();

        // add shop group
        var shopGroup15Result = assertDoesNotThrow(
                ()->testControllerHelperService.shopGroupControllerCreateNew(
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
        assertThat(shopGroup15Result).isNotNull();
        assertThat(shopGroup15Result.getErrorCode()).isEqualTo(0);
        shopGroup15 = shopGroup15Result.getPayload();

        // create shop group 17
        var shopGroup17Result = assertDoesNotThrow(
                ()->testControllerHelperService.shopGroupControllerCreateNew(
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
        assertThat(shopGroup17Result).isNotNull();
        assertThat(shopGroup17Result.getErrorCode()).isEqualTo(0);
        shopGroup17 = shopGroup17Result.getPayload();
    }

    @BeforeEach
    public void clear() {
        mongoTemplate.remove(Work.class).all();
    }


    @Test
    public void testForFailing() {
        // create a new work
        var failForMandatoryField = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(workTypes.get(0).id())
                                .build()
                )
        );
        assertThat(failForMandatoryField).isNotNull();
        // check that message contains the needed field
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("title");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("description");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("locationId");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("shopGroupId");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("subsystem");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("group");
        assertThat(failForMandatoryField.getErrorMessage()).containsIgnoringCase("urgency");
    }

    @Test
    public void checkSAssignedToState() {
        // custom field id for subsystem
        var subsystemCustomField = helperService.getCustomFiledByName(workTypes.getFirst(), "subsystem");
        var subsystemLovValuesList = lovService.findAllByDomainAndFieldName(    LOVDomainTypeDTO.Work, tecDomain.id(), workTypes.getFirst().id(), "subsystem");
        // get the custom field for the group
        var groupCustomField = helperService.getCustomFiledByName(workTypes.getFirst(), "group");
        var groupLovValuesList = lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Work, tecDomain.id(), workTypes.getFirst().id(), "group");
        // get the custom field for the urgency
        var urgencyCustomField = helperService.getCustomFiledByName(workTypes.getFirst(), "urgency");
        var urgencyLovValuesList = lovService.findAllByDomainAndFieldName(LOVDomainTypeDTO.Work, tecDomain.id(), workTypes.getFirst().id(), "urgency");

        // create a new work
        var newWorkResult = assertDoesNotThrow(
                ()->testControllerHelperService.workControllerCreateNew(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        tecDomain.id(),
                        NewWorkDTO
                                .builder()
                                .workTypeId(workTypes.getFirst().id())
                                .title("Work 1")
                                .description("Work 1 description")
                                .locationId(location10)
                                .shopGroupId(shopGroup15)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO.builder()
                                                        .id(subsystemCustomField.id())
                                                        .value(ValueDTO.builder().type(ValueTypeDTO.String).value(subsystemLovValuesList.getFirst().id()).build())
                                                        .build(),
                                                WriteCustomFieldDTO.builder()
                                                        .id(groupCustomField.id())
                                                        .value(ValueDTO.builder().type(ValueTypeDTO.String).value(groupLovValuesList.getFirst().id()).build())
                                                        .build(),
                                                WriteCustomFieldDTO.builder()
                                                        .id(urgencyCustomField.id())
                                                        .value(ValueDTO.builder().type(ValueTypeDTO.String).value(urgencyLovValuesList.getFirst().id()).build())
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(helperService.checkStatusOnWork(tecDomain.id(), newWorkResult.getPayload(), WorkflowStateDTO.Created)).isTrue();
        // check fi the workflow is in the 'created' state
//        var workResult = assertDoesNotThrow(
//                ()->testControllerHelperService.workControllerFindWorkById(
//                        mockMvc,
//                        status().isOk(),
//                        Optional.of("user1@slac.stanford.edu"),
//                        tecDomain.id(),
//                        newWorkResult.getPayload(),
//                        WorkDetailsOptionDTO.builder().build()
//                )
//        );
//        assertThat(workResult).isNotNull();
//        assertThat(workResult.getPayload()).isNotNull();
//        assertThat(workResult.getPayload().currentStatus().status()).isEqualTo(WorkflowStateDTO.Created);

    }
}
