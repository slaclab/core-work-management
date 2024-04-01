package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkTypeInitTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private WorkService workService;
    @Autowired
    private WorkTypeRepository workTypeRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
    }

    @Test
    public void initTest() {
        InitWorkType initWorkType = new InitWorkType(lovService, workService);
        assertDoesNotThrow(initWorkType::changeSet);
        var allWorkType = assertDoesNotThrow(
                () -> workService.findAllWorkTypes()
        );
        assertThat(allWorkType)
                .isNotEmpty()
                .extracting(WorkTypeDTO::title).containsExactly(
                        "General Issues",
                        "Software Issues",
                        "Hardware Issues"
                );
    }
}
