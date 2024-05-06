package edu.stanford.slac.core_work_management.service;

import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.model.*;
import jakarta.validation.ConstraintViolationException;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DomainServiceTest {
    @Autowired
    DomainService domainService;
    @Autowired
    MongoTemplate mongoTemplate;


    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
    }

    @Test
    public void testCreateNewDomain() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();
    }

    @Test
    public void testFetchDomainById() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        DomainDTO domainDTO = assertDoesNotThrow(
                ()-> domainService.findById(domainId)
        );
        assertThat(domainDTO).isNotNull();
        assertThat(domainDTO.id()).isEqualTo(domainId);
        assertThat(domainDTO.name()).isEqualTo("test-domain");
        assertThat(domainDTO.description()).isEqualTo("Test domain description");
    }

    @Test
    public void testFetchDomainByWrongId() {
        DomainNotFound domainNotFoundException = assertThrows(
                DomainNotFound.class,
                ()-> domainService.findById("wrong id")
        );
        assertThat(domainNotFoundException).isNotNull();
        assertThat(domainNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void testFindAll() {
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            String domainId = assertDoesNotThrow(
                    () -> domainService.createNew(
                            NewDomainDTO.builder()
                                    .name("TEST domain %s".formatted(finalI))
                                    .build()
                    )
            );
            assertThat(domainId).isNotNull().isNotEmpty();
        }

        List<DomainDTO> domainDTOList = assertDoesNotThrow(
                ()-> domainService.finAll()
        );
        for (int i = 0; i < 10; i++) {
            assertThat(domainDTOList.get(i).name()).isEqualTo("test-domain-%s".formatted(i));
        }
    }

    @Test
    public void failedToInsertDuplicatedName() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                ()-> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(exception.getErrorCode()).isEqualTo(-1);
        assertThat(exception.getErrorDomain()).contains("DomainService::createNew");
    }

    @Test
    public void failToCreateDomainWithoutMandatoryField() {
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                ()-> domainService.createNew(
                        NewDomainDTO.builder()
                                .build()
                )
        );
        assertThat(exception.getConstraintViolations()).hasSize(1);
    }
}
