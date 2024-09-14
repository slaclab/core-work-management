package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Set;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@AllArgsConstructor
@Profile({"init-domain", "init-default-data"})
@ChangeUnit(id = "init-tec-domain", order = "1001", author = "bisegni")
public class M1001_InitTECDomain {
    private final LOVService lovService;
    private final DomainService domainService;

    @Execution
    public void changeSet() {
        var tecDomain = initTECDomain();
        log.info("TEC domain created with id {} ", tecDomain.id());
    }


    @RollbackExecution
    public void rollback() {
    }

    /**
     * Initialize the TEC domain
     */
    public DomainDTO initTECDomain() {
        var domain = wrapCatch(
                () -> domainService.createNewAndGet(
                        NewDomainDTO
                                .builder()
                                .name("TEC")
                                .description("TEC")
                                .workflowImplementations(
                                        Set.of(
                                                "ReportWorkflow",
                                                "RequestWorkflow",
                                                "RecordWorkflow"
                                        )
                                )
                                .build()
                ),
                -1
        );

        // create work types
        createHardwareReportWT(domain);
        return domain;
    }

    private void createHardwareRequestWT(DomainDTO domain) {
        // create the hardware request work type
        var workflow = domain.workflows().stream().filter(w -> w.implementation().contains("RequestWorkflow")).findFirst().orElse(null);
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The workflow ReportWorkflow is not present in the domain")
                        .errorDomain("M1001_InitTECDomain::createHardwareReportWT")
                        .build(),
                () -> workflow != null
        );

        // create hardware request work type
    }

    /**
     * Create the hardware report work type
     *
     * @param domain the domain id
     */
    private void createHardwareReportWT(DomainDTO domain) {
        var workflow = domain.workflows().stream().filter(w -> w.implementation().contains("ReportWorkflow")).findFirst().orElse(null);
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The workflow ReportWorkflow is not present in the domain")
                        .errorDomain("M1001_InitTECDomain::createHardwareReportWT")
                        .build(),
                () -> workflow != null
        );

        var newHardwareReportId = domainService.createNew(
                domain.id(),
                NewWorkTypeDTO.builder()
                        .title("Hardware Request")
                        .description("It is used to request a new hardware job")
                        .workflowId(workflow.id())
                        .validatorName("TECHardwareRequestValidation")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder().label("Group").description("Group").valueType(ValueTypeDTO.LOV).additionalMappingInfo("ProjectGroup").group("General Information").isMandatory(true).build(),
                                        WATypeCustomFieldDTO.builder().label("Assigned To").description("Assigned To").valueType(ValueTypeDTO.String).group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Estimated Hrs").description("Estimated Hrs").valueType(ValueTypeDTO.Double).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("PA Number").description("PA Number").valueType(ValueTypeDTO.String).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Watch & Wait Date").description("Watch & Wait Date").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("W&W Comment").description("W&W Comment").valueType(ValueTypeDTO.String).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Created By").description("Created By").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Created Date").description("Created Date").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Request Description").description("Request Description").valueType(ValueTypeDTO.String).group("Background Information").isMandatory(true).build(),
                                        WATypeCustomFieldDTO.builder().label("Attachments and Files").description("Attachments and Files").valueType(ValueTypeDTO.Attachments).group("Action").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Request Customers").description("Request Customers").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Urgency").description("Urgency").valueType(ValueTypeDTO.LOV).additionalMappingInfo("UrgencyGroup").group("Other").isMandatory(true).build(),
                                        WATypeCustomFieldDTO.builder().label("High Opr Priority").description("High Opr Priority").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Micro").description("Micro").valueType(ValueTypeDTO.LOV).additionalMappingInfo("MicroGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Primary").description("Primary").valueType(ValueTypeDTO.LOV).additionalMappingInfo("PrimaryGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Unit").description("Unit").valueType(ValueTypeDTO.LOV).additionalMappingInfo("UnitGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Pv Name").description("Pv Name").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Action Item Review Date").description("Action Item Review Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Date Due Next").description("Date Due Next").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("CEF Request Submitted").description("CEF Request Submitted").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("CEF Tracking No.").description("CEF Tracking No.").valueType(ValueTypeDTO.Number).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Building").description("Building").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Building Manager").description("Building Manager").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Asst Bldg Manager").description("Asst Bldg Manager").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Facility").description("Facility").valueType(ValueTypeDTO.LOV).additionalMappingInfo("FacilityGroup").group("FacilityGroup").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Issue Priority").description("Issue Priority").valueType(ValueTypeDTO.LOV).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Area Mgr Rev Date").description("Area Mgr Rev Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Area Mgr Rev Comments").description("Area Mgr Rev Comments").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Facilities FAMIS Information").description("Facilities FAMIS Information").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build()
                                )
                        )
                        .build()
        );
    }
}