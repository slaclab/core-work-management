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
        NewWorkTypeDTO.builder()
                .title("Hardware Request")
                .description("It is used to request a new hardware job")
                .workflowId(workflow.id())
                .validatorName("TECHardwareRequestValidation")
                .customFields(
                        List.of(
                                WATypeCustomFieldDTO.builder().label("Scheduling Priority").description("Scheduling Priority").valueType(ValueTypeDTO.LOV).group("Scheduling").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("PMM Date").description("PMM Date").valueType(ValueTypeDTO.LOV).group("Scheduling").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Job Description").description("Job Description").valueType(ValueTypeDTO.String).group("Background Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Time (hrs)").description("Time (hrs)").valueType(ValueTypeDTO.Double).group("Scheduling").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Planned Start Date & Time").description("Planned Start Date & Time").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Planned Stop Date & Time").description("Planned Stop Date & Time").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Access Requirements").description("Access Requirements").valueType(ValueTypeDTO.LOV).group("Special Instructions and Dependencies").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("PPS Zone").description("PPS Zone").valueType(ValueTypeDTO.LOV).group("Special Instructions and Dependencies").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("RPFO Survey").description("RPFO Survey").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Radiation Removal Survey").description("Radiation Removal Survey").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Radiation Work Permit").description("Radiation Work Permit").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Other Issues").description("Other Issues").valueType(ValueTypeDTO.String).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Radiation Safety Work Control Form").description("Radiation Safety Work Control Form").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Lock and Tag").description("Lock and Tag").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("ODH Safety Work Control Form").description("ODH Safety Work Control Form").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("JSA").description("JSA").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Penetration Permit").description("Penetration Permit").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Hot Work Permit").description("Hot Work Permit").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("PPS Interlocked Hazard Checkout").description("PPS Interlocked Hazard Checkout").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Unreviewed Safety Issue (USI)").description("Unreviewed Safety Issue (USI)").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Additional Safety Information").description("Additional Safety Information").valueType(ValueTypeDTO.String).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("RSWCF Attachments").description("RSWCF Attachments").valueType(ValueTypeDTO.Attachments).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Specify Requirements").description("Specify Requirements").valueType(ValueTypeDTO.String).group("Requirements").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Estimated WPC Completion Date").description("Estimated WPC Completion Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("WPC Conditions Defined").description("WPC Conditions Defined").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Attachments and Files").description("Attachments and Files").valueType(ValueTypeDTO.Attachments).group("Action").isMandatory(false).build(),
//                                WATypeCustomFieldDTO.builder().label("WPC Project").description("WPC Project").valueType(ValueTypeDTO.Boolean).group("Action").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Area").description("Area").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Area Mgr").description("Area Mgr").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Subsystem").description("Subsystem").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Shop Main").description("Shop Main").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Shop Alt").description("Shop Alt").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Group").description("Group").valueType(ValueTypeDTO.LOV).group("General Information").isMandatory(true).build(),
                                WATypeCustomFieldDTO.builder().label("Created By").description("Created By").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Created Date").description("Created Date").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Modified By").description("Modified By").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Modified Date").description("Modified Date").valueType(ValueTypeDTO.String).group("Metadata").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Safety Issue").description("Safety Issue").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Number of Persons").description("Number of Persons").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Ongoing").description("Ongoing").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Minimum Hours").description("Minimum Hours").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Person Hours").description("Person Hours").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Toco Time").description("Toco Time").valueType(ValueTypeDTO.Double).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Beam Requirements").description("Beam Requirements").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Beam Comment").description("Beam Comment").valueType(ValueTypeDTO.String).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Invasive Comment").description("Invasive Comment").valueType(ValueTypeDTO.String).group("Other Dependencies").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Misc Comments").description("Misc Comments").valueType(ValueTypeDTO.String).group("Job Comments").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Feedback Comments").description("Feedback Comments").valueType(ValueTypeDTO.String).group("Job Comments").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Feedback Priority").description("Feedback Priority").valueType(ValueTypeDTO.LOV).group("Job Comments").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Micro").description("Micro").valueType(ValueTypeDTO.LOV).group("IOC/MICRO Depot").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Primary").description("Primary").valueType(ValueTypeDTO.LOV).group("IOC/MICRO Depot").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Unit").description("Unit").valueType(ValueTypeDTO.LOV).group("IOC/MICRO Depot").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Micro Other").description("Micro Other").valueType(ValueTypeDTO.LOV).group("IOC/MICRO Depot").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("DEPOT").description("DEPOT").valueType(ValueTypeDTO.LOV).group("IOC/MICRO Depot").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Building").description("Building").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Building Manager").description("Building Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Asst Bldg Manager").description("Asst Bldg Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Visual Number").description("Visual Number").valueType(ValueTypeDTO.String).group("FAMIS Info").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Task Manager").description("Task Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                WATypeCustomFieldDTO.builder().label("Job Change Comments").description("Job Change Comments").valueType(ValueTypeDTO.String).group("Comments").isMandatory(false).build()
                        )
                )
                .build();
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
                        .title("Hardware Report")
                        .description("It is used to report a problem")
                        .workflowId(workflow.id())
                        .validatorName("TECHardwareReportValidation")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder()
                                                .label("Subsystem")
                                                .description("Subsystem Group")
                                                .valueType(ValueTypeDTO.String)
                                                .group("General Information")
                                                .isMandatory(true)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Group")
                                                .description("Group")
                                                .valueType(ValueTypeDTO.String)
                                                .group("General Information")
                                                .isMandatory(true)
                                                .build(),
                                        //Estimated Hrs
                                        WATypeCustomFieldDTO.builder()
                                                .label("Estimated Hrs")
                                                .description("Estimated Hrs to complete the work")
                                                .valueType(ValueTypeDTO.Double)
                                                .group("Scheduling")
                                                .build(),
                                        // PA Number
                                        WATypeCustomFieldDTO.builder()
                                                .label("PA Number")
                                                .description("PA Number")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Scheduling")
                                                .build(),
                                        // Watch & Wait Date
                                        WATypeCustomFieldDTO.builder()
                                                .label("Watch & Wait Date")
                                                .description("Watch & Wait Date")
                                                .valueType(ValueTypeDTO.DateTime)
                                                .group("Scheduling")
                                                .build(),
                                        // W&W Comment
                                        WATypeCustomFieldDTO.builder()
                                                .label("W&W Comment")
                                                .description("W&W Comment")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Scheduling")
                                                .build(),
                                        // Hardware Jobs
                                        WATypeCustomFieldDTO.builder()
                                                .label("Hardware Jobs")
                                                .description("Hardware Jobs")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Action")
                                                .build(),
                                        // Software Jobs
                                        WATypeCustomFieldDTO.builder()
                                                .label("Software Jobs")
                                                .description("Software Jobs")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Action")
                                                .build(),
                                        // Radiation Safety Work Control Forms
                                        WATypeCustomFieldDTO.builder()
                                                .label("Radiation Safety Work Control Forms")
                                                .description("Radiation Safety Work Control Forms")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Action")
                                                .build(),
                                        // Solutions/Tasks
                                        WATypeCustomFieldDTO.builder()
                                                .label("Solutions/Tasks")
                                                .description("Solutions/Tasks")
                                                .valueType(ValueTypeDTO.String)
                                                .group("Action")
                                                .build(),
                                        // Beamlost Time
                                        WATypeCustomFieldDTO.builder()
                                                .label("Beamlost Time")
                                                .description("Beamlost Time")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Request Customers
                                        WATypeCustomFieldDTO.builder()
                                                .label("Request Customers")
                                                .description("Request Customers")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // High Opr Priority
                                        WATypeCustomFieldDTO.builder()
                                                .label("High Opr Priority")
                                                .description("High Opr Priority")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        // Pv Name
                                        WATypeCustomFieldDTO.builder()
                                                .label("Pv Name")
                                                .description("Pv Name")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Action Item Review Date
                                        WATypeCustomFieldDTO.builder()
                                                .label("Action Item Review Date")
                                                .description("Action Item Review Date")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.DateTime)
                                                .build(),
                                        // Date Due Next
                                        WATypeCustomFieldDTO.builder()
                                                .label("Date Due Next")
                                                .description("Date Due Next")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.Date)
                                                .build(),
                                        // CEF Request Submitted
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Request Submitted")
                                                .description("CEF Request Submitted")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        // CEF Tracking No.
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Tracking No.")
                                                .description("CEF Tracking No.")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Building
                                        WATypeCustomFieldDTO.builder()
                                                .label("Building")
                                                .description("Building")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Building Manager
                                        WATypeCustomFieldDTO.builder()
                                                .label("Building Manager")
                                                .description("Building Manager")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Asst Bldg Manager
                                        WATypeCustomFieldDTO.builder()
                                                .label("Asst Bldg Manager")
                                                .description("Asst Bldg Manager")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Area Mgr Rev Date
                                        WATypeCustomFieldDTO.builder()
                                                .label("Area Mgr Rev Date")
                                                .description("Area Mgr Rev Date")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.Date)
                                                .build(),
                                        // Area Mgr Rev Comments
                                        WATypeCustomFieldDTO.builder()
                                                .label("Area Mgr Rev Comments")
                                                .description("Area Mgr Rev Comments")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // Facilities FAMIS Information
                                        WATypeCustomFieldDTO.builder()
                                                .label("Facilities FAMIS Information")
                                                .description("Facilities FAMIS Information")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Urgency")
                                                .description("Urgency of the issue")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .isMandatory(true)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Micro")
                                                .description("Micro")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Primary")
                                                .description("Primary")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Unit")
                                                .description("Unit")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Facility")
                                                .description("Facility")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Issue Priority")
                                                .description("Issue priority")
                                                .group("Other")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        // add Radiation Safety Work Control Forms needed form
                                        WATypeCustomFieldDTO.builder()
                                                .label("radiationControlForm")
                                                .description("Radiation Safety Work Control Forms needed")
                                                .group("Action")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .isMandatory(false)
                                                .build()
                                )
                        )
                        .build()
        );

        // associate lov to custom fields
        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "subsystem",
                "SubsystemGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "group",
                "ProjectGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "urgency",
                "UrgencyGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "micro",
                "MicroGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "primary",
                "PrimaryGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "unit",
                "UnitGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "facility",
                "FacilityGroup"
        );

        lovService.associateDomainFieldToGroupName(
                LOVDomainTypeDTO.Work,
                domain.id(),
                newHardwareReportId,
                "issuePriority",
                "IssuePriorityGroup"
        );

    }
}