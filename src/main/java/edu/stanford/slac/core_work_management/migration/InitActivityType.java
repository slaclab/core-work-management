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
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@AllArgsConstructor
@Profile({"nit-work-activity-type"})
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

    private void createActivityTypes() {
        createLOVValues();
        manageHardwareActivity();

        var softwareActivity = NewActivityTypeDTO.builder()
                .title("Software Task")
                .description("An issue with currently installed accelerator software")
                .build();
        String softwareActivityId = workService.createNew(softwareActivity);
        var generalActivity = NewActivityTypeDTO.builder()
                .title("General Task")
                .description("A general task that does not fall under hardware or software")
                .build();
        String generalActivityId = workService.createNew(generalActivity);


    }

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
    }
    private void manageHardwareActivity() {
        var hardwareActivity = NewActivityTypeDTO.builder()
                .title("Hardware Task")
                .description("An issue with currently installed accelerator hardware")
                .customFields(
                        List.of(
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

        String hardwareActivityId = workService.createNew(hardwareActivity);
        manageHardwareActivityLOV(
                activityTypeRepository
                        .findById(hardwareActivityId)
                        .orElseThrow(
                                () -> ActivityTypeNotFound.notFoundById().activityTypeId(hardwareActivityId).build()
                        )
        );
    }


    /**
     * This method is used to manage the hardware activity LOV
     *
     * @param hardwareActivity the hardware activity to manage
     */
    private void manageHardwareActivityLOV(ActivityType hardwareActivity) {
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            hardwareActivity.getId(),
                            "Access Requirements",
                            "AccessRequirementsGroup"
                    );
                    return null;
                },
                -1
        );

        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Activity,
                            hardwareActivity.getId(),
                            "Beam Requirements",
                            "BeamRequirementsGroup"
                    );
                    return null;
                },
                -1
        );
    }

    private Optional<String> getCustomFieldLOVReferenceByFieldName(ActivityType activityType, String fieldName) {
        return activityType.getCustomFields().stream()
                .filter(customField -> customField.getName().equals(fieldName))
                .findFirst()
                .map(ActivityTypeCustomField::getLovFieldReference);
    }
}
