package edu.stanford.slac.core_work_management.controller;

import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.model.Domain;
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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DomainControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
    }

    @Test
    public void createNewDomain() {
        var createNewDomainResult = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerCreateNewDomain(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewDomainDTO
                                .builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .workflowImplementations(Set.of("DummyParentWorkflow"))
                                .build()
                )
        );

        assertThat(createNewDomainResult).isNotNull();
    }

    @Test
    public void getDomainById() {
        var createNewDomainResult = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerCreateNewDomain(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewDomainDTO
                                .builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .workflowImplementations(Set.of("DummyParentWorkflow"))
                                .build()
                )
        );

        assertThat(createNewDomainResult).isNotNull();
        assertThat(createNewDomainResult.getPayload()).isNotNull();

        var fullDomain = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerFindById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        createNewDomainResult.getPayload()
                )
        );

        assertThat(fullDomain).isNotNull();
        assertThat(fullDomain.getPayload()).isNotNull();
        assertThat(fullDomain.getPayload().id()).isEqualTo(createNewDomainResult.getPayload());
        assertThat(fullDomain.getPayload().name()).isEqualTo("test-domain");
        assertThat(fullDomain.getPayload().description()).isEqualTo("Test domain description");
        assertThat(fullDomain.getPayload().workflows()).hasSize(1);
        assertThat(fullDomain.getPayload().workflows().iterator().next().implementation()).isEqualTo("DummyParentWorkflow");
    }

    @Test
    public void findAllDomain() {
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            var createNewDomainResult = assertDoesNotThrow(
                    () -> testControllerHelperService.domainControllerCreateNewDomain(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            NewDomainDTO
                                    .builder()
                                    .name("TEST domain %s".formatted(finalI))
                                    .description("Test domain description")
                                    .workflowImplementations(Set.of("DummyParentWorkflow"))
                                    .build()
                    )
            );
            assertThat(createNewDomainResult).isNotNull();
        }

        var listAllDomain = assertDoesNotThrow(
                () -> testControllerHelperService.domainControllerFindAll(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );

        assertThat(listAllDomain).isNotNull();
        assertThat(listAllDomain.getPayload()).isNotNull();
        assertThat(listAllDomain.getPayload().size()).isEqualTo(10);
    }
}
