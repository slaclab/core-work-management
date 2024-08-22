package edu.stanford.slac.core_work_management.service;

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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test",  "async-ops"})
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
        mongoTemplate.remove(new Query(), ActivityType.class);
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

        for (WorkTypeStatusStatisticsDTO workType: statMap){
            var statForWTypeId = workType.status();
            statForWTypeId.stream().filter(stat->stat.status()==WorkStatusDTO.New).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.New)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Closed)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.Closed)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.InProgress)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.InProgress)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Review)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.Review)));
            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.ScheduledJob)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.ScheduledJob)));
        }
    }


    @Test
    public void createNewWorkType() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull();
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        // retrieve created work type
        var foundWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domainId, newWorkTypeId)
        );
        assertThat(foundWorkType).isNotNull();
        assertThat(foundWorkType.id()).isEqualTo(newWorkTypeId);
        assertThat(foundWorkType.domainId()).isEqualTo(domainId);

    }

    @Test
    public void createNewActivityType() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull();
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);
        String newActivityTypeId = assertDoesNotThrow(
                () -> domainService.ensureActivityType(
                        domainId,
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotNull();
        // retrieve created activity type
        var foundActivityType = assertDoesNotThrow(
                () -> domainService.findActivityTypeById(domainId, newWorkTypeId,newWorkTypeId)
        );
        assertThat(foundActivityType).isNotNull();
        assertThat(foundActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(foundActivityType.domainId()).isEqualTo(domainId);
        assertThat(foundActivityType.workTypeId()).isEqualTo(newWorkTypeId);
    }

    @Test
    public void updateActivityType() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .build()
                )
        );
        assertThat(domainId).isNotNull();
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);
        String newActivityTypeId = assertDoesNotThrow(
                () -> domainService.ensureActivityType(
                        domainId,
                        newWorkTypeId,
                        NewActivityTypeDTO
                                .builder()
                                .title("Activity 1")
                                .description("Activity 1 description")
                                .build()
                )
        );
        assertThat(newActivityTypeId).isNotNull();
        //update activity type
        assertDoesNotThrow(
                () -> domainService.updateActivityType(
                        domainId,
                        newWorkTypeId,
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 updated")
                                .description("Activity 1 description updated")
                                .customFields(
                                        List.of(
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field1")
                                                        .description("custom field1 description")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field2")
                                                        .description("custom field2 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        // retrieve and check the full activity type
        var fullUpdatedActivityType = assertDoesNotThrow(
                () -> domainService.findActivityTypeById(domainId, newWorkTypeId,newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1");
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field2");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField2");
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field2 description");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isFalse();

        //update activity type adding a new custom filed and modifying the other
        ActivityTypeDTO finalFullUpdatedActivityType = fullUpdatedActivityType;
        assertDoesNotThrow(
                () -> domainService.updateActivityType(
                        domainId,
                        newWorkTypeId,
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 re-updated")
                                .description("Activity 1 description re-updated")
                                .customFields(
                                        List.of(
                                                finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                finalFullUpdatedActivityType.customFields().get(1).toBuilder()
                                                        .label("custom field2 updated")
                                                        .description("custom field2 description updated")
                                                        .valueType(ValueTypeDTO.Number)
                                                        .isLov(true)
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedActivityType = assertDoesNotThrow(
                () -> domainService.findActivityTypeById(domainId, newWorkTypeId, newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(3);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field2 updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField2"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field2 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Number);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isTrue();
        assertThat(fullUpdatedActivityType.customFields().get(2).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(2).name()).isEqualTo("customField3");
        assertThat(fullUpdatedActivityType.customFields().get(2).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedActivityType.customFields().get(2).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(2).isMandatory()).isFalse();

        //update activity type removing an attribute
        assertDoesNotThrow(
                () -> domainService.updateActivityType(
                        domainId,
                        newWorkTypeId,
                        newActivityTypeId,
                        UpdateActivityTypeDTO
                                .builder()
                                .title("Activity 1 re-updated")
                                .description("Activity 1 description re-updated")
                                .customFields(
                                        List.of(
                                                finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isLov(false)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedActivityType = assertDoesNotThrow(
                () -> domainService.findActivityTypeById(domainId, newWorkTypeId, newActivityTypeId)
        );
        assertThat(fullUpdatedActivityType).isNotNull();
        assertThat(fullUpdatedActivityType.id()).isEqualTo(newActivityTypeId);
        assertThat(fullUpdatedActivityType.title()).isEqualTo("Activity 1 re-updated");
        assertThat(fullUpdatedActivityType.description()).isEqualTo("Activity 1 description re-updated");
        assertThat(fullUpdatedActivityType.customFields()).isNotNull();
        assertThat(fullUpdatedActivityType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedActivityType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedActivityType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedActivityType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedActivityType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedActivityType.customFields().get(1).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedActivityType.customFields().get(1).name()).isEqualTo("customField3");
        assertThat(fullUpdatedActivityType.customFields().get(1).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedActivityType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedActivityType.customFields().get(1).isMandatory()).isFalse();
    }
}
