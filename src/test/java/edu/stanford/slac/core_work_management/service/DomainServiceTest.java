package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper;
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
import java.util.Set;
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
@ActiveProfiles({"test", "async-ops"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DomainServiceTest {
    @Autowired
    private DomainMapper domainMapper;
    @Autowired
    private DomainService domainService;
    @Autowired
    private LOVService lovService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private WorkRepository workRepository;
    @Autowired
    private WorkTypeRepository workTypeRepository;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Domain.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
    }

    @Test
    public void testCreateNewDomain() {
        String domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
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
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        DomainDTO domainDTO = assertDoesNotThrow(
                () -> domainService.findById(domainId)
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
                () -> domainService.findById("wrong id")
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
                                    .description("Test domain description")
                                    .workflowImplementations(
                                            Set.of(
                                                    "DummyParentWorkflow"
                                            )
                                    )
                                    .build()
                    )
            );
            assertThat(domainId).isNotNull().isNotEmpty();
        }

        List<DomainDTO> domainDTOList = assertDoesNotThrow(
                () -> domainService.finAll()
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
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotNull().isNotEmpty();

        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .name("TEST domain")
                                .description("Test domain description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
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
                () -> domainService.createNew(
                        NewDomainDTO.builder()
                                .build()
                )
        );
        assertThat(exception.getConstraintViolations()).hasSize(3);
    }

    @Test
    public void testStatistic() {
//        String domainId = assertDoesNotThrow(
//                () -> domainService.createNew(
//                        NewDomainDTO.builder()
//                                .name("dom1")
//                                .description("Test domain description")
//                                .build()
//                )
//        );
//        assertThat(domainId).isNotNull();
//
//        WorkType newWorkType1 = assertDoesNotThrow(
//                () -> workTypeRepository.save(
//                        WorkType
//                                .builder()
//                                .title("workType-1")
//                                .description("workType-1")
//                                .build()
//                )
//        );
//        assertThat(newWorkType1).isNotNull();
//        WorkType newWorkType2 = assertDoesNotThrow(
//                () -> workTypeRepository.save(
//                        WorkType
//                                .builder()
//                                .title("workType-2")
//                                .description("workType-2")
//                                .build()
//                )
//        );
//        assertThat(newWorkType2).isNotNull();
//        List<WorkType> workTypes = Arrays.asList(newWorkType1, newWorkType2);
//
//        // Create work in new state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-new-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkflowStateDTO.New).build()).build())
//            );
//            Assertions.assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in closed state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-closed-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Closed).build()).build())
//            );
//            Assertions.assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in review state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-review-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Review).build()).build())
//            );
//            Assertions.assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in progress state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-progress-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.InProgress).build()).build())
//            );
//            Assertions.assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in scheduled state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = workTypes.get(ThreadLocalRandom.current().nextInt(workTypes.size())).getId();
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId(domainId).title("w-scheduled-job-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.ScheduledJob).build()).build())
//            );
//            Assertions.assertThat(savedWork).isNotNull();
//        }
//
//        DomainDTO domainDTO = assertDoesNotThrow(()->domainService.findById(domainId));
//
//        assertDoesNotThrow(()->domainService.updateDomainStatistics(domainDTO.id()));
//
//        await().atMost(30, SECONDS).pollDelay(2, SECONDS).until(
//                () -> {
//                    var domainUpdated = assertDoesNotThrow(()->domainService.findById(domainDTO.id()));
//                    return domainUpdated.lastModifiedDate().isAfter(domainDTO.lastModifiedDate());
//                }
//        );
//
//        var domainUpdated = assertDoesNotThrow(()->domainService.findById(domainId));
//        var statMap = domainUpdated.workTypeStatusStatistics();
//        assertThat(statMap).isNotNull().isNotEmpty();
//
//        for (WorkTypeStatusStatisticsDTO workType: statMap){
//            var statForWTypeId = workType.status();
//            statForWTypeId.stream().filter(stat->stat.status()==WorkStatusDTO.New).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.New)));
//            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Closed)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.Closed)));
//            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.InProgress)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.InProgress)));
//            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.Review)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.Review)));
//            statForWTypeId.stream().filter(stat->stat.status().equals(WorkStatusDTO.ScheduledJob)).findFirst().ifPresent(stat->Assertions.assertThat(stat.count()).isEqualTo(workRepository.countByDomainIdAndWorkTypeIdAndCurrentStatus_StatusIs(domainUpdated.id(), workType.workType().id(), WorkStatus.ScheduledJob)));
//        }
    }


    @Test
    public void createNewWorkType() {
        DomainDTO domain = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domain).isNotNull();
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domain.id(),
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(domain.workflows().stream().findFirst().get().id())
                                .validatorName("validator/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        // retrieve created work type
        var foundWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domain.id(), newWorkTypeId)
        );
        assertThat(foundWorkType).isNotNull();
        assertThat(foundWorkType.id()).isEqualTo(newWorkTypeId);
        assertThat(foundWorkType.domainId()).isEqualTo(domain.id());

    }

    @Test
    public void updateActivityType() {
        DomainDTO domain = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domain).isNotNull();
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domain.id(),
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .validatorName("validator/DummyParentValidation.groovy")
                                .workflowId(domain.workflows().stream().findFirst().get().id())
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull().contains(newWorkTypeId);

        //update activity type
        assertDoesNotThrow(
                () -> domainService.updateWorkType(
                        domain.id(),
                        newWorkTypeId,
                        UpdateWorkTypeDTO
                                .builder()
                                .title("work type 1 updated")
                                .description("work type 1 description updated")
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
        var fullUpdatedWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domain.id(), newWorkTypeId)
        );
        assertThat(fullUpdatedWorkType).isNotNull();
        assertThat(fullUpdatedWorkType.id()).isEqualTo(newWorkTypeId);
        assertThat(fullUpdatedWorkType.title()).isEqualTo("work type 1 updated");
        assertThat(fullUpdatedWorkType.description()).isEqualTo("work type 1 description updated");
        assertThat(fullUpdatedWorkType.customFields()).isNotNull();
        assertThat(fullUpdatedWorkType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedWorkType.customFields().get(0).label()).isEqualTo("custom field1");
        assertThat(fullUpdatedWorkType.customFields().get(0).name()).isEqualTo("customField1");
        assertThat(fullUpdatedWorkType.customFields().get(0).description()).isEqualTo("custom field1 description");
        assertThat(fullUpdatedWorkType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedWorkType.customFields().get(0).isMandatory()).isTrue();
        assertThat(fullUpdatedWorkType.customFields().get(1).label()).isEqualTo("custom field2");
        assertThat(fullUpdatedWorkType.customFields().get(1).name()).isEqualTo("customField2");
        assertThat(fullUpdatedWorkType.customFields().get(1).description()).isEqualTo("custom field2 description");
        assertThat(fullUpdatedWorkType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedWorkType.customFields().get(1).isMandatory()).isFalse();

        //update work type adding a new custom filed and modifying the other
        WorkTypeDTO finalFullUpdatedActivityType = fullUpdatedWorkType;
        assertDoesNotThrow(
                () -> domainService.updateWorkType(
                        domain.id(),
                        newWorkTypeId,
                        UpdateWorkTypeDTO
                                .builder()
                                .title("work 1 re-updated")
                                .description("work 1 description re-updated")
                                .customFields(
                                        List.of(
                                                domainMapper.map(finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(false)
                                                        .build()),
                                                domainMapper.map(finalFullUpdatedActivityType.customFields().get(1).toBuilder()
                                                        .label("custom field2 updated")
                                                        .description("custom field2 description updated")
                                                        .valueType(ValueTypeDTO.Number)
                                                        .isMandatory(true)
                                                        .build()),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domain.id(), newWorkTypeId)
        );
        assertThat(fullUpdatedWorkType).isNotNull();
        assertThat(fullUpdatedWorkType.id()).isEqualTo(newWorkTypeId);
        assertThat(fullUpdatedWorkType.title()).isEqualTo("work 1 re-updated");
        assertThat(fullUpdatedWorkType.description()).isEqualTo("work 1 description re-updated");
        assertThat(fullUpdatedWorkType.customFields()).isNotNull();
        assertThat(fullUpdatedWorkType.customFields().size()).isEqualTo(3);
        assertThat(fullUpdatedWorkType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedWorkType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedWorkType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedWorkType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedWorkType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedWorkType.customFields().get(1).label()).isEqualTo("custom field2 updated");
        assertThat(fullUpdatedWorkType.customFields().get(1).name()).isEqualTo("customField2"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedWorkType.customFields().get(1).description()).isEqualTo("custom field2 description updated");
        assertThat(fullUpdatedWorkType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Number);
        assertThat(fullUpdatedWorkType.customFields().get(1).isMandatory()).isTrue();
        assertThat(fullUpdatedWorkType.customFields().get(2).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedWorkType.customFields().get(2).name()).isEqualTo("customField3");
        assertThat(fullUpdatedWorkType.customFields().get(2).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedWorkType.customFields().get(2).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedWorkType.customFields().get(2).isMandatory()).isFalse();

        //update activity type removing an attribute
        assertDoesNotThrow(
                () -> domainService.updateWorkType(
                        domain.id(),
                        newWorkTypeId,
                        UpdateWorkTypeDTO
                                .builder()
                                .title("work 1 re-updated")
                                .description("work 1 description re-updated")
                                .customFields(
                                        List.of(
                                                domainMapper.map(finalFullUpdatedActivityType.customFields().get(0).toBuilder()
                                                        .label("custom field1 updated")
                                                        .description("custom field1 description updated")
                                                        .valueType(ValueTypeDTO.String)
                                                        .isMandatory(false)
                                                        .build()),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        fullUpdatedWorkType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domain.id(), newWorkTypeId)
        );
        assertThat(fullUpdatedWorkType).isNotNull();
        assertThat(fullUpdatedWorkType.id()).isEqualTo(newWorkTypeId);
        assertThat(fullUpdatedWorkType.title()).isEqualTo("work 1 re-updated");
        assertThat(fullUpdatedWorkType.description()).isEqualTo("work 1 description re-updated");
        assertThat(fullUpdatedWorkType.customFields()).isNotNull();
        assertThat(fullUpdatedWorkType.customFields().size()).isEqualTo(2);
        assertThat(fullUpdatedWorkType.customFields().get(0).label()).isEqualTo("custom field1 updated");
        assertThat(fullUpdatedWorkType.customFields().get(0).name()).isEqualTo("customField1"); // notice this is correct because if the label is found it is not updated
        assertThat(fullUpdatedWorkType.customFields().get(0).description()).isEqualTo("custom field1 description updated");
        assertThat(fullUpdatedWorkType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.String);
        assertThat(fullUpdatedWorkType.customFields().get(0).isMandatory()).isFalse();
        assertThat(fullUpdatedWorkType.customFields().get(1).label()).isEqualTo("custom field3");
        assertThat(fullUpdatedWorkType.customFields().get(1).name()).isEqualTo("customField3");
        assertThat(fullUpdatedWorkType.customFields().get(1).description()).isEqualTo("custom field3 description");
        assertThat(fullUpdatedWorkType.customFields().get(1).valueType()).isEqualTo(ValueTypeDTO.Boolean);
        assertThat(fullUpdatedWorkType.customFields().get(1).isMandatory()).isFalse();
    }

    /**
     * Test that the work type read out has a list of custom fields
     */
    @Test
    public void testWorkTypeReadOutHasLOVElementList() {
        List<String> lovElementIds = lovService.createNew(
                "LovGroup1",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("LovGroup1::Value1")
                                .description("LovGroup1::Value1 description")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LovGroup1::Value2")
                                .description("LovGroup1::Value2 description")
                                .build()
                )
        );
        // create domain
        DomainDTO domain = assertDoesNotThrow(
                () -> domainService.createNewAndGet(
                        NewDomainDTO.builder()
                                .name("dom1")
                                .description("Test domain description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domain).isNotNull();
        //update activity type removing an attribute
        String newWorkId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domain.id(),
                        NewWorkTypeDTO
                                .builder()
                                .title("work 1")
                                .description("work 1 description")
                                .workflowId(domain.workflows().stream().findFirst().get().id())
                                .validatorName("validator/DummyParentValidation.groovy")
                                .customFields(
                                        List.of(
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("group one filed")
                                                        .description("custom field with lov description")
                                                        .valueType(ValueTypeDTO.LOV)
                                                        .isMandatory(false)
                                                        .build(),
                                                WATypeCustomFieldDTO
                                                        .builder()
                                                        .label("custom field3")
                                                        .description("custom field3 description")
                                                        .valueType(ValueTypeDTO.Boolean)
                                                        .isMandatory(false)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        // associate the lov element to the custom field
        // associate LOV with the custom field
        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newWorkId,
                "groupOneFiled",
                "LovGroup1"
        );

        // retrieve the work type
        WorkTypeDTO workType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domain.id(), newWorkId)
        );
        assertThat(workType).isNotNull();
        assertThat(workType.customFields()).isNotNull();
        assertThat(workType.customFields().size()).isEqualTo(2);
        assertThat(workType.customFields().get(0).label()).isEqualTo("group one filed");
        assertThat(workType.customFields().get(0).valueType()).isEqualTo(ValueTypeDTO.LOV);
        assertThat(workType.customFields().get(0).lovValues()).isNotNull();
        assertThat(workType.customFields().get(0).lovValues().size()).isEqualTo(2);
        assertThat(workType.customFields().get(0).lovValues().get(0).value()).isEqualTo("LovGroup1::Value1");
        assertThat(workType.customFields().get(0).lovValues().get(0).description()).isEqualTo("LovGroup1::Value1 description");
        assertThat(workType.customFields().get(0).lovValues().get(1).value()).isEqualTo("LovGroup1::Value2");
        assertThat(workType.customFields().get(0).lovValues().get(1).description()).isEqualTo("LovGroup1::Value2 description");
    }
}
