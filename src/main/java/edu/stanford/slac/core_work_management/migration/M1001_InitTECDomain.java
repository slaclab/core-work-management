package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
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
                                                "ReportWorkflow"
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

        var newProblemReportId = domainService.createNew(
                domain.id(),
                NewWorkTypeDTO.builder()
                        .title("Hardware Report")
                        .description("It is used to report a problem")
                        .workflowId(workflow.id())
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder()
                                                .label("Subsystem")
                                                .description("Subsystem Group")
                                                .valueType(ValueTypeDTO.String)
                                                .group("General Information")
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Group")
                                                .description("Group")
                                                .valueType(ValueTypeDTO.String)
                                                .group("General Information")
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
                                                .build()
                                )
                        )
                        .build()
        );

        // associate lov to custom fields
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "subsystem",
                            "SubsystemGroup"
                    );
                    return null;
                },
                -2
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "group",
                            "ProjectGroup"
                    );
                    return null;
                },
                -3
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "urgency",
                            "UrgencyGroup"
                    );
                    return null;
                },
                -4
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "micro",
                            "MicroGroup"
                    );
                    return null;
                },
                -5
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "primary",
                            "PrimaryGroup"
                    );
                    return null;
                },
                -6
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "unit",
                            "UnitGroup"
                    );
                    return null;
                },
                -7
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "facility",
                            "FacilityGroup"
                    );
                    return null;
                },
                -8
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            domain.id(),
                            newProblemReportId,
                            "issuePriority",
                            "IssuePriorityGroup"
                    );
                    return null;
                },
                -9
        );
    }
}