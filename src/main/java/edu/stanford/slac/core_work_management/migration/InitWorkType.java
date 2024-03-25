package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.util.List;

@AllArgsConstructor
@Profile({"init-work-activity-type"})
@ChangeUnit(id = "init-work-type", order = "1001", author = "bisegni")
public class InitWorkType {
    private final WorkTypeRepository workTypeRepository;
    private final ActivityTypeRepository activityTypeRepository;
    @Execution
    public void changeSet() {

        createWorkTypes();
    }

    @RollbackExecution
    public void rollback() {

    }
    private void createWorkTypes() {
        List<WorkType> workTypes = List.of(
                WorkType.builder()
                        .title("HW Maintenance")
                        .description("An issue with currently installed accelerator hardware")
                        .build(),
                WorkType.builder()
                        .title("HW Installation / Removal")
                        .description("Used for scheduling PM on accelerator hardware and infrastructure")
                        .build(),
                WorkType.builder()
                        .title("HW Problem")
                        .description("An issue with currently installed accelerator hardware")
                        .build(),
                WorkType.builder()
                        .title("SW Maintenance")
                        .description("An issue with currently installed accelerator software")
                        .build(),
                WorkType.builder()
                        .title("SW Installation/ Removal")
                        .description("Used for scheduling PM on accelerator software")
                        .build(),
                WorkType.builder()
                        .title("SW Problem")
                        .description("An issue with currently installed accelerator software")
                        .build(),
                WorkType.builder()
                        .title("Project enhancement request")
                        .description("A request for a new feature(HW/SW) or enhancement to an existing feature (HW/SW)")
                        .build(),
                WorkType.builder()
                        .title("Administrative Activity")
                        .description(
                                """
                                Used for documenting precautions/issues that may impact planned work; also scheduling other
                                impactful work in accelerator areas like surveys, inspections and tours
                                """
                        )
                        .build()
        );
        for (WorkType workType : workTypes) {
            workTypeRepository.ensureWorkType(workType);
        }
    }
}
