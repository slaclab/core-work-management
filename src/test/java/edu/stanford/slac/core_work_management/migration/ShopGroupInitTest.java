package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
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
public class ShopGroupInitTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private ShopGroupService shopGroupService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
    }

    @Test
    public void initTest() {
        M1000_InitDomain initDomain = new M1000_InitDomain(lovService, domainService);
        assertDoesNotThrow(initDomain::changeSet);
        M2000InitShopGroup initShopGroup = new M2000InitShopGroup(domainService, shopGroupService);
        assertDoesNotThrow(initShopGroup::changeSet);

        assertDoesNotThrow(()->domainService.findByName("TEC"));

        var sg = assertDoesNotThrow(()->shopGroupService.findAll());
        assertThat(sg).hasSize(2);
        assertThat(sg.get(0).name()).isEqualTo("Accelerator Physics");
        assertThat(sg.get(1).name()).isEqualTo("Accelerator Controls Systems");
    }
}
