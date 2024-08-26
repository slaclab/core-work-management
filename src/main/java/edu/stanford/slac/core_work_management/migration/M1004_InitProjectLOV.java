package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLOVElementDTO;
import edu.stanford.slac.core_work_management.service.LOVService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@AllArgsConstructor
@Profile({"init-default-data"})
@ChangeUnit(id = "init-project-slot-lov", order = "1003", author = "bisegni")
public class M1004_InitProjectLOV {
    private final LOVService lovService;

    @Execution
    public void changeSet() {
        createLovForProjectField();
    }

    @RollbackExecution
    public void rollback() {

    }

    /**
     * This method is used to create the activity types
     */
    private void createLovForProjectField() {
        lovService.createNew(
                "Project",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("CBXFEL")
                                .description("CBXFEL")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COMMON")
                                .description("COMMON")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DASEL")
                                .description("DASEL")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET")
                                .description("FACET")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET User Area")
                                .description("FACET User Area")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS")
                                .description("LCLS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS-II")
                                .description("LCLS-II")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS-II HE")
                                .description("LCLS-II HE")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("OTHER")
                                .description("OTHER")
                                .build()
                )
        );


        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Work,
                            null,
                            "project",
                            "Project"
                    );
                    return null;
                },
                -1
        );
    }
}
