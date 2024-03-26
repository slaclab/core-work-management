package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.ActivityTypeCustomField;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@AllArgsConstructor
@Profile({"init-work-activity-type"})
@ChangeUnit(id = "init-activity-type", order = "1002", author = "bisegni")
public class InitActivityType {
    private final LOVService lovService;
    private final WorkService workService;
    private final ActivityTypeRepository activityTypeRepository;

    @Execution
    public void changeSet() {
        createActivityTypes();
    }

    @RollbackExecution
    public void rollback() {

    }

    /**
     * This method is used to create the activity types
     */
    private void createActivityTypes() {
        createLOVValues();
        manageGeneralActivity();
        manageSoftwareActivity();
        manageHardwareActivity();
    }

    /**
     * This method is used to create the LOV values
     */
    private void createLOVValues() {
        lovService.createNew(
                "AccessRequirementsGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Any Access")
                                .description("Any access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Controlled Access")
                                .description("Controlled access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Access")
                                .description("No access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Restricted Access")
                                .description("Restricted access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Permitted Access")
                                .description("Permitted access is allowed")
                                .build()
                )
        );

        lovService.createNew(
                "BeamRequirementsGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Beam")
                                .description("Beam is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Beam")
                                .description("No beam is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VVSs on")
                                .description("VVSs are on")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ACC trig")
                                .description("ACC is triggered")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("STBY Trig")
                                .description("STBY is triggered")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Requirement")
                                .description("No beam is allowed")
                                .build()
                )
        );

        lovService.createNew(
                "SchedulingPriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Benign")
                                .description("Benign work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PAMM")
                                .description("PAMM work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POMM")
                                .description("POMM work type")
                                .build()
                )
        );

        lovService.createNew(
                "TaskPriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("1")
                                .description("Highest priority")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("5")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("6")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("7")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("8")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("9")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("10")
                                .description("Lowest priority")
                                .build()
                )
        );

        //{Yes, No, Unknown, Do Not Need}
        lovService.createNew(
                "DocSolutionGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Yes")
                                .description("Yes")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No")
                                .description("No")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Unknown")
                                .description("Unknown")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Do Not Need")
                                .description("Do Not Need")
                                .build()
                )
        );
    }

    private void manageSoftwareActivity() {
        var softwareActivityId = workService.createNew(
                NewActivityTypeDTO.builder()
                        .title("Software Task")
                        .description("An issue with currently installed accelerator software")
                        .customFields(
                                List.of(
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Scheduling Priority")
                                                .description("The scheduling priority for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Time Comments")
                                                .description("The time comments for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Access Requirements")
                                                .description("The access requirements for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Other Issues")
                                                .description("Other issues related to the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Beam Requirements")
                                                .description("The beam requirements for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Beam Comment")
                                                .description("The beam comment for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Invasive")
                                                .description("The invasive status for the hardware job")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Invasive Comment")
                                                .description("The invasive comment for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Test Plan")
                                                .description("The test plan for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Backout Plan")
                                                .description("The backout plan for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Systems Required")
                                                .description("The systems required for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Systems Affected")
                                                .description("The systems affected for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Risk/Benefit")
                                                .description("The risk/benefit for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("Dependencies")
                                                .description("The dependencies for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .label("CD Review Date")
                                                .description("The CD review date for the hardware job")
                                                .valueType(ValueTypeDTO.Date)
                                                .build()

                                )
                        )
                        .build()
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            softwareActivityId,
                            "schedulingPriority",
                            "SchedulingPriorityGroup"
                    );
                    return null;
                },
                -2
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            softwareActivityId,
                            "accessRequirements",
                            "AccessRequirementsGroup"
                    );
                    return null;
                },
                -2
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            softwareActivityId,
                            "beamRequirements",
                            "BeamRequirementsGroup"
                    );
                    return null;
                },
                -3
        );
    }

    private void manageGeneralActivity() {
        var generalActivity = NewActivityTypeDTO.builder()
                .title("General Task")
                .description("A general task that does not fall under hardware or software")
                .customFields(
                        of(
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Task Priority")
                                        .description("The task priority for the General job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Task Skill Set")
                                        .description("The task skill set for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Percentage completed")
                                        .description("The percentage completed for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Module")
                                        .description("The module for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Old Serial Number")
                                        .description("The old serial number for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("New Serial Number")
                                        .description("The new serial number for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Drawing")
                                        .description("The drawing for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Doc Solution")
                                        .description("The document for the General job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Date RTC Checked")
                                        .description("The date RTC checked for the General job")
                                        .valueType(ValueTypeDTO.Date)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Feedback Priority")
                                        .description("The feedback priority for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build()
                        )
                )
                .build();
        String generalActivityId = wrapCatch(
                () -> workService.createNew(generalActivity),
                -1
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            generalActivityId,
                            "taskPriority",
                            "TaskPriorityGroup"
                    );
                    return null;
                },
                -2
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            generalActivityId,
                            "docSolution",
                            "DocSolutionGroup"
                    );
                    return null;
                },
                -3
        );
    }

    /**
     * This method is used to manage the hardware activity LOV and custom fields
     */
    private void manageHardwareActivity() {
        var hardwareActivity = NewActivityTypeDTO.builder()
                .title("Hardware Task")
                .description("An issue with currently installed accelerator hardware")
                .customFields(
                        List.of(
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Scheduling Priority")
                                        .description("The scheduling priority for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Access Requirements")
                                        .description("The access requirements for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Other Issues")
                                        .description("Other issues related to the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Rad Safety Work Ctl Form")
                                        .description("The radiation safety work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Lock and Tag")
                                        .description("The lock and tag for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("PPS Interlocked")
                                        .description("The PPS interlocked for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Atmospheric Work Control Form")
                                        .description("The atmospheric work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Electric Sys Work Ctl Form")
                                        .description("The electrical work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Additional Safety Information")
                                        .description("Additional safety information for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Specify Requirements")
                                        .description("Specify the requirements for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Release Conditions Defined")
                                        .description("The safety plan for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Safety Issue")
                                        .description("The safety issue for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Number of Persons")
                                        .description("The number of persons for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Ongoing")
                                        .description("The ongoing status for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Minimum Hours")
                                        .description("The minimum hours for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Person Hours")
                                        .description("The person hours for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Toco Time")
                                        .description("The toco time for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Feedback Priority")
                                        .description("The toco time units for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Beam Requirements")
                                        .description("The beam requirements for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Beam Comment")
                                        .description("The beam comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Invasive")
                                        .description("The invasive status for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Invasive Comment")
                                        .description("The invasive comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Misc Job Comments")
                                        .description("The miscellaneous job comments for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Feedback Priority")
                                        .description("The feedback priority for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Feedback Priority Comment")
                                        .description("The feedback priority comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Micro")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Primary")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Micro Other")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .label("Visual Number")
                                        .description("???")
                                        .valueType(ValueTypeDTO.Number)
                                        .build()
                        )
                )
                .build();

        String hardwareActivityId = wrapCatch(
                () -> workService.createNew(hardwareActivity),
                -1
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            hardwareActivityId,
                            "schedulingPriority",
                            "SchedulingPriorityGroup"
                    );
                    return null;
                },
                -2
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            hardwareActivityId,
                            "accessRequirements",
                            "AccessRequirementsGroup"
                    );
                    return null;
                },
                -2
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            hardwareActivityId,
                            "beamRequirements",
                            "BeamRequirementsGroup"
                    );
                    return null;
                },
                -3
        );
    }
}
