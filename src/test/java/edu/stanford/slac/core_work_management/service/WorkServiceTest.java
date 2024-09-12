package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.*;
import edu.stanford.slac.core_work_management.model.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.bson.Document;
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

import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkServiceTest {
    @Autowired
    DomainService domainService;
    @Autowired
    WorkService workService;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    LocationService locationService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    LOVService lovService;

    private DomainDTO fullDomain;
    private WorkflowDTO parentWorkflow;
    private WorkflowDTO childWorkflow;
    private String shopGroupId;
    private String alternateShopGroupId;
    private String locationId;
    private String locationIdOnAlternateDomain;
    private String domainId;
    private String alternateDomainId;
    private List<LOVElementDTO> projectLovValues = null;
    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.getCollection("jv_head_id").deleteMany(new Document());
        mongoTemplate.getCollection("jv_snapshots").deleteMany(new Document());
        mongoTemplate.remove(new Query(), Domain.class);
        domainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Test Domain")
                                .description("Test Domain Description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow",
                                                "DummyChildWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(domainId).isNotEmpty();

        // fetch full domain
        fullDomain = assertDoesNotThrow(
                () -> domainService.findById(domainId)
        );
        parentWorkflow = fullDomain.workflows().stream().filter(w -> w.implementation().equals("DummyParentWorkflow")).findFirst().get();
        childWorkflow = fullDomain.workflows().stream().filter(w -> w.implementation().equals("DummyChildWorkflow")).findFirst().get();

        alternateDomainId = assertDoesNotThrow(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("Alternate Test Domain")
                                .description("Alternate Test Domain Description")
                                .workflowImplementations(
                                        Set.of(
                                                "DummyParentWorkflow"
                                        )
                                )
                                .build()
                )
        );
        assertThat(alternateDomainId).isNotEmpty();

        mongoTemplate.remove(new Query(), Location.class);
        mongoTemplate.remove(new Query(), WorkType.class);
        mongoTemplate.remove(new Query(), Work.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
        mongoTemplate.remove(new Query(), ShopGroup.class);
        shopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                domainId,
                                NewShopGroupDTO.builder()
                                        .name("shop1")
                                        .description("shop1 user[2-3]")
                                        .users(
                                                of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user2@slac.stanford.edu")
                                                                .build(),
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user3@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(shopGroupId).isNotEmpty();

        alternateShopGroupId =
                assertDoesNotThrow(
                        () -> shopGroupService.createNew(
                                alternateDomainId,
                                NewShopGroupDTO.builder()
                                        .name("shop2")
                                        .description("shop1 user[2-3]")
                                        .users(
                                                of(
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user2@slac.stanford.edu")
                                                                .build(),
                                                        ShopGroupUserInputDTO.builder()
                                                                .userId("user3@slac.stanford.edu")
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                );
        AssertionsForClassTypes.assertThat(alternateShopGroupId).isNotEmpty();

        locationId = assertDoesNotThrow(
                () -> locationService.createNew(
                        domainId,
                        NewLocationDTO.builder()
                                .name("SLAC")
                                .description("SLAC National Accelerator Laboratory")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(locationId).isNotEmpty();

        locationIdOnAlternateDomain = assertDoesNotThrow(
                () -> locationService.createNew(
                        alternateDomainId,
                        NewLocationDTO.builder()
                                .name("Alternate location")
                                .description("Alternate location description")
                                .locationManagerUserId("user1@slac.stanford.edu")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(locationIdOnAlternateDomain).isNotEmpty();
    }

    @Test
    public void createNewWork() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();
    }

    @Test
    public void updateWorkOK() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Work 1")
                                .description("Work 1 description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        assertDoesNotThrow(
                () -> workService.update(
                        domainId,
                        newWorkId,
                        UpdateWorkDTO
                                .builder()
                                .title("Update work 1")
                                .description("Update work 1 description")
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
    }

    @Test
    public void updateWorkFailOnInvalidLocationForDomainId() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Work 1")
                                .description("Work 1 description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        LocationNotFound invalidLocationForDomainException = assertThrows(
                LocationNotFound.class,
                () -> workService.update(
                        domainId,
                        newWorkId,
                        UpdateWorkDTO
                                .builder()
                                .title("Update work 1")
                                .description("Update work 1 description")
                                .locationId(locationIdOnAlternateDomain)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(invalidLocationForDomainException).isNotNull();
        assertThat(invalidLocationForDomainException.getErrorCode()).isEqualTo(-4);
    }

    @Test
    public void createNewWorkFailWithLocationInvalidForDomain() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();

        // create the work in the alternate domain with the location of the main domain
        LocationNotFound invalidLocationException = assertThrows(
                LocationNotFound.class,
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationIdOnAlternateDomain)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(invalidLocationException).isNotNull();
        assertThat(invalidLocationException.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void createNewWorkFailWithShopGroupInvalidForDomain() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();

        // create the work in the alternate domain with the location of the main domain
        ShopGroupNotFound invalidLocationException = assertThrows(
                ShopGroupNotFound.class,
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(alternateShopGroupId)
                                .build()
                )
        );
        assertThat(invalidLocationException).isNotNull();
        assertThat(invalidLocationException.getErrorCode()).isEqualTo(-4);
    }

    @Test
    public void createNewWorkAndGetIt() {
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        var foundWork = assertDoesNotThrow(
                () -> workService.findWorkById(domainId, newWorkId, WorkDetailsOptionDTO.builder().build())
        );
        assertThat(foundWork).isNotNull();
        assertThat(foundWork.id()).isNotNull();
        assertThat(foundWork.domain().id()).isEqualTo(domainId);
    }

    @Test
    public void errorTryToGetWorkWithBadId() {
        var workNotFoundException = assertThrows(
                WorkNotFound.class,
                () -> workService.findWorkById(domainId, "bad id", WorkDetailsOptionDTO.builder().build())
        );
        assertThat(workNotFoundException).isNotNull();
        assertThat(workNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createNewSubWorkOK() {
        // create work type for children
        String newChildrenWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Children work type")
                                .description("Update the documentation description")
                                .workflowId(childWorkflow.id())
                                .validatorName("validation/DummyChildValidation.groovy")
                                .build()
                )
        );
        //create work type
        String newParentWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Parent work type")
                                .description("Update the documentation description")
                                .childWorkTypeIds(Set.of(newChildrenWorkTypeId))
                                .validatorName("validation/DummyParentValidation.groovy")
                                .workflowId(parentWorkflow.id())
                                .build()
                )
        );
        assertThat(newParentWorkTypeId).isNotNull();
        // create work plan
        var newParentWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Parent work")
                                .description("Parent work description")
                                .workTypeId(newParentWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newParentWorkId).isNotEmpty();
        // now create children
        var newChildrenWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Child work")
                                .description("Child work description")
                                .workTypeId(newChildrenWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .parentWorkId(newParentWorkId)
                                .build()
                )
        );
        assertThat(newChildrenWorkId).isNotEmpty();
        // get full children
        var fullChildrenWork = assertDoesNotThrow(
                () -> workService.findWorkById(domainId, newChildrenWorkId, WorkDetailsOptionDTO.builder().build())
        );
        assertThat(fullChildrenWork).isNotNull();
        assertThat(fullChildrenWork.id()).isNotNull();
        assertThat(fullChildrenWork.domain().id()).isEqualTo(domainId);
        assertThat(fullChildrenWork.parentWorkId()).isEqualTo(newParentWorkId);
    }


    @Test
    public void checkWorkCustomAttributeLOV() {
        // create lov
        List<String> lovElementIds = lovService.createNew(
                "SubsystemGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("ACM")
                                .description("ACM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("AIDA")
                                .description("AIDA")
                                .build()
                )
        );
        //create work type
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.createNew(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Parent work type")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .customFields(
                                        List.of(
                                                WATypeCustomFieldDTO.builder()
                                                        .label("Subsystem")
                                                        .description("Subsystem Group")
                                                        .valueType(ValueTypeDTO.LOV)
                                                        .group("General Information")
                                                        .lovGroup("SubsystemGroup")
                                                        .isMandatory(true)
                                                        .build(),
                                                WATypeCustomFieldDTO.builder()
                                                        .label("attributeOne")
                                                        .description("Attribute One")
                                                        .valueType(ValueTypeDTO.String)
                                                        .group("General Information")
                                                        .lovGroup("SubsystemGroup")
                                                        .isMandatory(true)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();

        // associate LOV with the custom field
        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domainId,
                newWorkTypeId,
                "subsystem",
                "SubsystemGroup"
        );

        // retrieve the full work type
        var workType = assertDoesNotThrow(
                () -> domainService.findWorkTypeById(domainId, newWorkTypeId)
        );
        assertThat(workType).isNotNull();

        // create work plan
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .customFieldValues(
                                        List.of(
                                                WriteCustomFieldDTO.builder()
                                                        .id(workType.customFields().getFirst().id())
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .type(ValueTypeDTO.LOV)
                                                                        .value(lovElementIds.getFirst())
                                                                        .build()
                                                        )
                                                        .build(),
                                                WriteCustomFieldDTO.builder()
                                                        .id(workType.customFields().getLast().id())
                                                        .value(
                                                                ValueDTO.builder()
                                                                        .type(ValueTypeDTO.String)
                                                                        .value("a good string value")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(newWorkId).isNotEmpty();

        // fetch full work
        var fullWork = assertDoesNotThrow(
                () -> workService.findWorkById(domainId, newWorkId, WorkDetailsOptionDTO.builder().build())
        );
        assertThat(fullWork).isNotNull();
        assertThat(fullWork.id()).isNotNull();
        assertThat(fullWork.domain().id()).isEqualTo(domainId);
        assertThat(fullWork.customFields()).isNotNull().hasSize(2);
        assertThat(fullWork.customFields().getFirst().value().value()).isEqualTo(lovElementIds.getFirst());
        assertThat(fullWork.customFields().getLast().value().value()).isEqualTo("a good string value");

    }

    @Test
    public void testWorkChanges() {
        // create base work
        String newWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        var newWorkId = assertDoesNotThrow(
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .build()
                )
        );
        assertThat(newWorkId).isNotNull();

        var foundWorkWithHistory = assertDoesNotThrow(
                () -> workService.findWorkById(domainId, newWorkId, WorkDetailsOptionDTO.builder().changes(true).build())
        );
        assertThat(foundWorkWithHistory).isNotNull();
        assertThat(foundWorkWithHistory.id()).isEqualTo(newWorkId);
        assertThat(foundWorkWithHistory.changesHistory()).isNotNull().hasSize(1);

        var foundWorkNoHistory = assertDoesNotThrow(
                () -> workService.findWorkById(domainId, newWorkId, WorkDetailsOptionDTO.builder().changes(false).build())
        );
        assertThat(foundWorkNoHistory).isNotNull();
        assertThat(foundWorkNoHistory.id()).isEqualTo(newWorkId);
        assertThat(foundWorkNoHistory.changesHistory()).isNotNull().isEmpty();
    }

    @Test
    public void savingWorkFailsWithWrongParentId() {
        // create base work type
        String newChildWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workflowId(childWorkflow.id())
                                .validatorName("validation/DummyChildValidation.groovy")
                                .build()
                )
        );
        assertThat(newChildWorkTypeId).isNotNull();

        String newParentWorkTypeId = assertDoesNotThrow(
                () -> domainService.ensureWorkType(
                        domainId,
                        NewWorkTypeDTO
                                .builder()
                                .title("find the documentation")
                                .description("find the documentation description")
                                .childWorkTypeIds(Set.of(newChildWorkTypeId))
                                .workflowId(parentWorkflow.id())
                                .validatorName("validation/DummyParentValidation.groovy")
                                .build()
                )
        );
        assertThat(newParentWorkTypeId).isNotNull();

        // type for the parent
        var workNotFound = assertThrows(
                WorkNotFound.class,
                () -> workService.createNew(
                        domainId,
                        NewWorkDTO
                                .builder()
                                .title("Update the documentation")
                                .description("Update the documentation description")
                                .workTypeId(newParentWorkTypeId)
                                .locationId(locationId)
                                .shopGroupId(shopGroupId)
                                .parentWorkId("bad id")
                                .build()
                )
        );
        assertThat(workNotFound).isNotNull();

    }
}
