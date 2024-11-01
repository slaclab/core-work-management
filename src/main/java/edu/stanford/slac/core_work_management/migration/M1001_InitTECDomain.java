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
        // create the hardware request work type
        var requestWorkflow = domain.workflows().stream().filter(w -> w.implementation().contains("RequestWorkflow")).findFirst().orElse(null);
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The workflow RequestWorkflow is not present in the domain")
                        .errorDomain("M1001_InitTECDomain::createHardwareReportWT")
                        .build(),
                () -> requestWorkflow != null
        );
        var reportWorkflow = domain.workflows().stream().filter(w -> w.implementation().contains("ReportWorkflow")).findFirst().orElse(null);
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The workflow ReportWorkflow is not present in the domain")
                        .errorDomain("M1001_InitTECDomain::createHardwareReportWT")
                        .build(),
                () -> reportWorkflow != null
        );
        var recordWorkflow = domain.workflows().stream().filter(w -> w.implementation().contains("RecordWorkflow")).findFirst().orElse(null);
        assertion(
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The workflow RecordWorkflow is not present in the domain")
                        .errorDomain("M1001_InitTECDomain::createHardwareReportWT")
                        .build(),
                () -> recordWorkflow != null
        );

        // create work types
        var newHardwareRequestId = domainService.createNew(
                domain.id(),
                NewWorkTypeDTO.builder()
                        .title("Hardware Request")
                        .description("It is used to request a new hardware job intervention")
                        .workflowId(requestWorkflow.id())
                        .validatorName("validation/TECHardwareRequestValidation.groovy")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder().label("schedulingPriority").description("Scheduling Priority").valueType(ValueTypeDTO.LOV).additionalMappingInfo("SchedulingPriorityGroup").group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("timeHrs").description("Time (hrs)").valueType(ValueTypeDTO.Double).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("plannedStartDateTime").description("Planned Start Date & Time").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("plannedStopDateTime").description("Planned Stop Date & Time").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("accessRequirements").description("Access Requirements").valueType(ValueTypeDTO.LOV).additionalMappingInfo("AccessRequirementsGroup").group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("ppsZone").description("PPS Zone").valueType(ValueTypeDTO.LOV).additionalMappingInfo("PPSZoneGroup").group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("rpfoSurvey").description("RPFO Survey").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("radiationRemovalSurvey").description("Radiation Removal Survey").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("radiationWorkPermit").description("Radiation Work Permit").valueType(ValueTypeDTO.Boolean).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("otherIssues").description("Other Issues").valueType(ValueTypeDTO.String).group("Special Instructions and Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("radiationSafetyWorkControlForm").description("Radiation Safety Work Control Form").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("lockAndTag").description("Lock and Tag").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("odhSafetyWorkControlForm").description("ODH Safety Work Control Form").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("jsa").description("JSA").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("penetrationPermit").description("Penetration Permit").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("hotWorkPermit").description("Hot Work Permit").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("ppsInterlockedHazardCheckout").description("PPS Interlocked Hazard Checkout").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("unreviewedSafetyIssue").description("Unreviewed Safety Issue (USI)").valueType(ValueTypeDTO.Boolean).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("additionalSafetyInformation").description("Additional Safety Information").valueType(ValueTypeDTO.String).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("rswcfAttachments").description("RSWCF Attachments").valueType(ValueTypeDTO.Attachments).group("Required Safety Forms or Controls").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("specifyRequirements").description("Specify Requirements").valueType(ValueTypeDTO.String).group("Requirements").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("estimatedWPCCompletionDate").description("Estimated WPC Completion Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("wpcConditionsDefined").description("WPC Conditions Defined").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("attachmentsAndFiles").description("Attachments and Files").valueType(ValueTypeDTO.Attachments).group("Action").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("subsystem").description("Subsystem").valueType(ValueTypeDTO.LOV).additionalMappingInfo("SubsystemGroup").group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("project").description("Project").valueType(ValueTypeDTO.LOV).additionalMappingInfo("ProjectGroup").group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("safetyIssue").description("Safety Issue").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("numberOfPersons").description("Number of Persons").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("ongoing").description("Ongoing").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("minimumHours").description("Minimum Hours").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("personHours").description("Person Hours").valueType(ValueTypeDTO.Number).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("tocoTime").description("Toco Time").valueType(ValueTypeDTO.Double).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("beamRequirements").description("Beam Requirements").valueType(ValueTypeDTO.Boolean).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("beamComment").description("Beam Comment").valueType(ValueTypeDTO.String).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("invasiveComment").description("Invasive Comment").valueType(ValueTypeDTO.String).group("Other Dependencies").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("miscComments").description("Misc Comments").valueType(ValueTypeDTO.String).group("Job Comments").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("feedbackComments").description("Feedback Comments").valueType(ValueTypeDTO.String).group("Job Comments").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("feedbackPriority").description("Feedback Priority").valueType(ValueTypeDTO.LOV).group("Job Comments").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("micro").description("Micro").valueType(ValueTypeDTO.LOV).additionalMappingInfo("MicroGroup").group("IOC/MICRO Depot").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("primary").description("Primary").valueType(ValueTypeDTO.LOV).additionalMappingInfo("PrimaryGroup").group("IOC/MICRO Depot").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("unit").description("Unit").valueType(ValueTypeDTO.LOV).additionalMappingInfo("UnitGroup").group("IOC/MICRO Depot").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("microOther").description("Micro Other").valueType(ValueTypeDTO.LOV).additionalMappingInfo("MicroGroup").group("IOC/MICRO Depot").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("building").description("Building").valueType(ValueTypeDTO.LOV).additionalMappingInfo("BuildingGroup").group("FAMIS Info").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("buildingManager").description("Building Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("asstBldgManager").description("Asst Bldg Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("visualNumber").description("Visual Number").valueType(ValueTypeDTO.String).group("FAMIS Info").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("taskManager").description("Task Manager").valueType(ValueTypeDTO.LOV).group("FAMIS Info").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("jobChangeComments").description("Job Change Comments").valueType(ValueTypeDTO.String).group("Comments").isMandatory(false).build()
                                )
                        )
                        .build()
        );
        var softwareRecordId = domainService.createNew(
                domain.id(),
                NewWorkTypeDTO.builder()
                        .title("Software Record")
                        .description("It is used to record a software issue")
                        .workflowId(recordWorkflow.id())
                        .validatorName("validation/TECSoftwareRecordValidation.groovy")
                        .childWorkTypeIds(Set.of(newHardwareRequestId))
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder().label("Complete").description("Complete").valueType(ValueTypeDTO.Boolean).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Feedback Comments").description("Feedback Comments").valueType(ValueTypeDTO.String).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Solver").description("Solver").valueType(ValueTypeDTO.Users).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Solution Type").description("Solution Type").additionalMappingInfo("SolutionTypeGroup").valueType(ValueTypeDTO.LOV).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("Solve Hours").description("Solve Hours").valueType(ValueTypeDTO.Double).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("DocSolution").description("Doc (Solution)").valueType(ValueTypeDTO.Boolean).isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("DocSolutionAttachments").description("Doc (Solution) Attachments").valueType(ValueTypeDTO.Attachments).isMandatory(false).build()
                                )
                        )
                        .build()
        );

        var newHardwareReportId = domainService.createNew(
                domain.id(),
                NewWorkTypeDTO.builder()
                        .title("Hardware Report")
                        .description("It is used to report an hardware issue")
                        .workflowId(reportWorkflow.id())
                        .validatorName("validation/TECHardwareReportValidation.groovy")
                        .childWorkTypeIds(Set.of(newHardwareRequestId, softwareRecordId))
                        .customFields(
                                List.of(
//                                        WATypeCustomFieldDTO.builder().label("group").description("Group").valueType(ValueTypeDTO.LOV).additionalMappingInfo("ProjectGroup").group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("project").description("Project").valueType(ValueTypeDTO.LOV).additionalMappingInfo("ProjectGroup").group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("subsystem").description("Subsystem").valueType(ValueTypeDTO.LOV).additionalMappingInfo("SubsystemGroup").group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("assignedTo").description("Assigned To").valueType(ValueTypeDTO.String).group("General Information").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("estimatedHrs").description("Estimated Hrs").valueType(ValueTypeDTO.Double).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("paNumber").description("PA Number").valueType(ValueTypeDTO.String).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("watchAndWaitDate").description("Watch & Wait Date").valueType(ValueTypeDTO.DateTime).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("wwComment").description("W&W Comment").valueType(ValueTypeDTO.String).group("Scheduling").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("attachmentsAndFiles").description("Attachments and Files").valueType(ValueTypeDTO.Attachments).group("Action").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("requestCustomers").description("Request Customers").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("urgency").description("Urgency").valueType(ValueTypeDTO.LOV).additionalMappingInfo("UrgencyGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("highOprPriority").description("High Opr Priority").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("micro").description("Micro").valueType(ValueTypeDTO.LOV).additionalMappingInfo("MicroGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("primary").description("Primary").valueType(ValueTypeDTO.LOV).additionalMappingInfo("PrimaryGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("unit").description("Unit").valueType(ValueTypeDTO.LOV).additionalMappingInfo("UnitGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("pvName").description("Pv Name").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("actionItemReviewDate").description("Action Item Review Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("dateDueNext").description("Date Due Next").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("cefRequestSubmitted").description("CEF Request Submitted").valueType(ValueTypeDTO.Boolean).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("cefTrackingNo").description("CEF Tracking No.").valueType(ValueTypeDTO.Number).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("building").description("Building").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("buildingManager").description("Building Manager").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("asstBldgManager").description("Asst Bldg Manager").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("facility").description("Facility").valueType(ValueTypeDTO.LOV).additionalMappingInfo("FacilityGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("issuePriority").description("Issue Priority").valueType(ValueTypeDTO.LOV).additionalMappingInfo("IssuePriorityGroup").group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("areaMgrRevDate").description("Area Mgr Rev Date").valueType(ValueTypeDTO.Date).group("Other").isMandatory(false).build(),
                                        WATypeCustomFieldDTO.builder().label("areaMgrRevComments").description("Area Mgr Rev Comments").valueType(ValueTypeDTO.String).group("Other").isMandatory(false).build()
                                )
                        )
                        .build()
        );
        return domain;
    }
}