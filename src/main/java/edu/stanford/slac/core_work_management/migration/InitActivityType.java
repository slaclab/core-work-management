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
        manageHardwareActivity();
        manageSoftwareActivity();
        manageGeneralActivity();
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
                                                .name("Scheduling Priority")
                                                .description("The scheduling priority for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Time Comments")
                                                .description("The time comments for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Access Requirements")
                                                .description("The access requirements for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Other Issues")
                                                .description("Other issues related to the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Beam Requirements")
                                                .description("The beam requirements for the hardware job")
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Beam Comment")
                                                .description("The beam comment for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Invasive")
                                                .description("The invasive status for the hardware job")
                                                .valueType(ValueTypeDTO.Boolean)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Invasive Comment")
                                                .description("The invasive comment for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Test Plan")
                                                .description("The test plan for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Backout Plan")
                                                .description("The backout plan for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Systems Required")
                                                .description("The systems required for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Systems Affected")
                                                .description("The systems affected for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Risk/Benefit")
                                                .description("The risk/benefit for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("Dependencies")
                                                .description("The dependencies for the hardware job")
                                                .valueType(ValueTypeDTO.String)
                                                .build(),
                                        ActivityTypeCustomFieldDTO.builder()
                                                .name("CD Review Date")
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
                            "Scheduling Priority",
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
                            "Access Requirements",
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
                            "Beam Requirements",
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
                                        .name("Task Priority")
                                        .description("The task priority for the General job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Task Skill Set")
                                        .description("The task skill set for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Percentage completed")
                                        .description("The percentage completed for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Module")
                                        .description("The module for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Old Serial Number")
                                        .description("The old serial number for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("New Serial Number")
                                        .description("The new serial number for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Drawing")
                                        .description("The drawing for the General job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Doc (Solution)")
                                        .description("The document for the General job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Date RTC Checked")
                                        .description("The date RTC checked for the General job")
                                        .valueType(ValueTypeDTO.Date)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Feedback Priority")
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
                            "Task Priority",
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
                            "Doc (Solution)",
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
                                        .name("Scheduling Priority")
                                        .description("The scheduling priority for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Access Requirements")
                                        .description("The access requirements for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Other Issues")
                                        .description("Other issues related to the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Rad Safety Work Ctl Form")
                                        .description("The radiation safety work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Lock and Tag")
                                        .description("The lock and tag for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("PPS Interlocked")
                                        .description("The PPS interlocked for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Atmospheric Work Control Form")
                                        .description("The atmospheric work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Electric Sys Work Ctl Form")
                                        .description("The electrical work control form for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Additional Safety Information")
                                        .description("Additional safety information for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Specify Requirements")
                                        .description("Specify the requirements for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Release Conditions Defined")
                                        .description("The safety plan for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Safety Issue")
                                        .description("The safety issue for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Number of Persons")
                                        .description("The number of persons for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Ongoing")
                                        .description("The ongoing status for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Minimum Hours")
                                        .description("The minimum hours for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Person Hours")
                                        .description("The person hours for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Toco Time")
                                        .description("The toco time for the hardware job")
                                        .valueType(ValueTypeDTO.Number)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Feedback Priority")
                                        .description("The toco time units for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Beam Requirements")
                                        .description("The beam requirements for the hardware job")
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Beam Comment")
                                        .description("The beam comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Invasive")
                                        .description("The invasive status for the hardware job")
                                        .valueType(ValueTypeDTO.Boolean)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Invasive Comment")
                                        .description("The invasive comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Misc Job Comments")
                                        .description("The miscellaneous job comments for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Feedback Priority")
                                        .description("The feedback priority for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Feedback Priority Comment")
                                        .description("The feedback priority comment for the hardware job")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Micro")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Primary")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Micro Other")
                                        .description("???")
                                        .valueType(ValueTypeDTO.String)
                                        .build(),
                                ActivityTypeCustomFieldDTO.builder()
                                        .name("Visual Number")
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
                            "Scheduling Priority",
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
                            "Access Requirements",
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
                            "Beam Requirements",
                            "BeamRequirementsGroup"
                    );
                    return null;
                },
                -3
        );
    }
}
