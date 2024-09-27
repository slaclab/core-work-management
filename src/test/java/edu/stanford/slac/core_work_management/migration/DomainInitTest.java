package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DomainInitTest {
    @Autowired
    LOVService lovService;
    @Autowired
    DomainService domainService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void clear() {
        mongoTemplate.remove(Domain.class).all();
        mongoTemplate.remove(WorkType.class).all();
        mongoTemplate.remove(LOVElement.class).all();
    }

    @Test
    public void test() {
        M1000_InitLOV initLOV = new M1000_InitLOV(lovService);
        assertDoesNotThrow(initLOV::initLOV);
        M1001_InitTECDomain initWorkType = new M1001_InitTECDomain(lovService, domainService);
        var tecDomain = assertDoesNotThrow(initWorkType::initTECDomain);
        assertThat(tecDomain).isNotNull();
    }
}
