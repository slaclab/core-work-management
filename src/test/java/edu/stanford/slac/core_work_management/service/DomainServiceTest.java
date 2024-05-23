package edu.stanford.slac.core_work_management.service;

import com.google.common.collect.ImmutableSet;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
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

import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.ImmutableList.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
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
    @Autowired
    WorkRepository workRepository;
    @Autowired
    WorkTypeRepository workTypeRepository;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), WorkType.class);
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

    @Test
    public void testStatistic() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull();

        WorkType newWorkType1 = assertDoesNotThrow(
                () -> workTypeRepository.save(
                        WorkType
                                .builder()
                                .title("workType-1")
                                .description("workType-1")
                                .build()
                )
        );
        assertThat(newWorkType1).isNotNull();
        WorkType newWorkType2 = assertDoesNotThrow(
                () -> workTypeRepository.save(
                        WorkType
                                .builder()
                                .title("workType-2")
                                .description("workType-2")
                                .build()
                )
        );
        assertThat(newWorkType2).isNotNull();
        List<WorkType> workTypes = Arrays.asList(newWorkType1, newWorkType2);

        // Create work in new state
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
            var savedWork = assertDoesNotThrow(
                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-new-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.New).build()).build())
            );
            Assertions.assertThat(savedWork).isNotNull();
        }

        // Create work in closed state
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
            var savedWork = assertDoesNotThrow(
                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-closed-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Closed).build()).build())
            );
            Assertions.assertThat(savedWork).isNotNull();
        }

        // Create work in review state
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
            var savedWork = assertDoesNotThrow(
                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-review-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Review).build()).build())
            );
            Assertions.assertThat(savedWork).isNotNull();
        }

        // Create work in progress state
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
            var savedWork = assertDoesNotThrow(
                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-progress-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.InProgress).build()).build())
            );
            Assertions.assertThat(savedWork).isNotNull();
        }

        // Create work in scheduled state
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
            var savedWork = assertDoesNotThrow(
                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-scheduled-job-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.ScheduledJob).build()).build())
            );
            Assertions.assertThat(savedWork).isNotNull();
        }

        DomainDTO domainDTO = assertDoesNotThrow(()->domainService.findById(domainId));

        assertDoesNotThrow(()->domainService.updateDomainStatistics(domainDTO.id()));

        await().atMost(30, SECONDS).pollDelay(2, SECONDS).until(
                () -> {
                    var domainUpdated = assertDoesNotThrow(()->domainService.findById(domainDTO.id()));
                    return domainUpdated.lastModifiedDate().isAfter(domainDTO.lastModifiedDate());
                }
        );

        var domainUpdated = assertDoesNotThrow(()->domainService.findById(domainId));
        var statMap = domainUpdated.workTypeStatusStatistics();
        assertThat(statMap).isNotNull().isNotEmpty();

        for (String workTypeId: statMap.keySet()){
            var statForWTypeId = statMap.get(workTypeId);
            statForWTypeId.stream().filter(stat->stat.status()==WorkStatusDTO.New).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workTypeId, WorkStatus.New)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Closed)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workTypeId, WorkStatus.Closed)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.InProgress)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workTypeId, WorkStatus.InProgress)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Review)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workTypeId, WorkStatus.Review)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.ScheduledJob)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workTypeId, WorkStatus.ScheduledJob)));
        }
    }
}
