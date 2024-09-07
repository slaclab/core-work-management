package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.controller.TestControllerHelperService;
import edu.stanford.slac.core_work_management.migration.M1001_InitTECDomain;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

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
    private WorkService workService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    // test tec domain
    private DomainDTO tecDomain = null;
    private List<WorkTypeDTO> workTypes = null;

    @BeforeAll
    public void init() {
        mongoTemplate.remove(Domain.class).all();
        mongoTemplate.remove(WorkType.class).all();
        mongoTemplate.remove(LOVElement.class).all();
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
}
