package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@AllArgsConstructor
@Profile({"init-work-activity-type", "init-default-data"})
@ChangeUnit(id = "init-work-type", order = "1001", author = "bisegni")
public class InitWorkType {
    private final LOVService lovService;
    private final WorkService workService;

    @Execution
    public void changeSet() {
        createLOV();
        manageHardwareIssues();
        manageSoftwareIssue();
        manageGeneralIssue();
    }

    @RollbackExecution
    public void rollback() {

    }

    public void createLOV() {
        //Facility: {Batch, Cater, Database, EPICS, Micro, PEPII, SCP, Touch Panels, Other}
        lovService.createNew(
                "FacilityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Batch")
                                .description("Batch")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Cater")
                                .description("Cater")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Database")
                                .description("Database")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EPICS")
                                .description("EPICS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Micro")
                                .description("Micro")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PEPII")
                                .description("PEPII")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SCP")
                                .description("SCP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Touch Panels")
                                .description("Touch Panels")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Other")
                                .description("Other")
                                .build()
                )
        );
        //Urgency: {Scheduled, Immediate, Downtime, Low Priority}
        lovService.createNew(
                "UrgencyGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Scheduled")
                                .description("Scheduled")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Immediate")
                                .description("Immediate")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Low Priority")
                                .description("Low Priority")
                                .build()
                )
        );
    }

    public void manageGeneralIssue() {
        var newGeneralIssueId = workService.createNew(
                NewWorkTypeDTO.builder()
                        .title("General Issues")
                        .description("An issue with currently installed accelerator hardware")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Request Submitted")
                                                .description("CEF request submission status")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Tracking No")
                                                .description("CEF tracking number")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Customer Priority")
                                                .description("The customer priority of the issue")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Customer Need By Date")
                                                .description("The customer need by date of the issue")
                                                .valueType(ValueTypeDTO.Date)
                                                .build()
                                )
                        )
                        .build()
        );
    }

    public void manageSoftwareIssue() {
        var newSoftwareIssueId = workService.createNew(
                NewWorkTypeDTO.builder()
                        .title("Software Issues")
                        .description("An issue with currently installed accelerator software")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder()
                                                .label("Urgency")
                                                .description("The urgency of the issue")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Facility")
                                                .description("The facility where the issue is located")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        //Display: text box
                                        WATypeCustomFieldDTO.builder()
                                                .label("Display")
                                                .description("The display where the issue is located")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        //Terminal Type: text box
                                        WATypeCustomFieldDTO.builder()
                                                .label("Terminal Type")
                                                .description("The terminal type where the issue is located")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        //Reproductible: {select}
                                        WATypeCustomFieldDTO.builder()
                                                .label("Reproductible")
                                                .description("The reproductible status of the issue")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build()
                                )
                        )
                        .build()
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            newSoftwareIssueId,
                            "facility",
                            "FacilityGroup"
                    );
                    return null;
                },
                -2
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            newSoftwareIssueId,
                            "urgency",
                            "UrgencyGroup"
                    );
                    return null;
                },
                -2
        );
    }

    public void manageHardwareIssues() {
        var newHardwareIssueId = workService.createNew(
                NewWorkTypeDTO.builder()
                        .title("Hardware Issues")
                        .description("An issue with currently installed accelerator hardware")
                        .customFields(
                                List.of(
                                        WATypeCustomFieldDTO.builder()
                                                .label("Urgency")
                                                .description("The urgency of the issue")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Micro")
                                                .description("???")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Primary")
                                                .description("???")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Unit")
                                                .description("???")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Pv Name")
                                                .description("???")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Date Due Next")
                                                .description("???")
                                                .valueType(ValueTypeDTO.Date)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Request Submitted")
                                                .description("CEF request submission status")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("CEF Tracking No")
                                                .description("CEF tracking number")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        WATypeCustomFieldDTO.builder()
                                                .label("Facility")
                                                .description("The facility where the issue is located")
                                                .valueType(ValueTypeDTO.String)
                                                .build()
                                )
                        )
                        .build()
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            newHardwareIssueId,
                            "facility",
                            "FacilityGroup"
                    );
                    return null;
                },
                -2
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            newHardwareIssueId,
                            "urgency",
                            "UrgencyGroup"
                    );
                    return null;
                },
                -2
        );
    }

}
