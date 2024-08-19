package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.LOVService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@AllArgsConstructor
@Profile({"init-default-data"})
@ChangeUnit(id = "init-bucket-slot-lov", order = "1003", author = "bisegni")
public class M1003_InitBucketTypeLOV {
    private final LOVService lovService;

    @Execution
    public void changeSet() {
        createLovForBucketSlot();
    }

    @RollbackExecution
    public void rollback() {

    }

    /**
     * This method is used to create the activity types
     */
    private void createLovForBucketSlot() {
        lovService.createNew(
                "BucketType",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("All Accelerators")
                                .description("All Accelerators")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("AOSD LCLS")
                                .description("AOSD LCLS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EIC")
                                .description("EIC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET")
                                .description("FACET")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET-II")
                                .description("FACET-II")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET-MD")
                                .description("FACET-MD")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS")
                                .description("LCLS")
                                .build()
                )
        );
        lovService.createNew(
                "BucketStatus",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("PAMM")
                                .description("PAMM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POMM")
                                .description("POMM")
                                .build()
                )
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Bucket,
                            "bucket",
                            "type",
                            "BucketType"
                    );
                    return null;
                },
                -2
        );
        wrapCatch(
                () -> {
                    lovService.associateDomainFieldToGroupName(
                            LOVDomainTypeDTO.Bucket,
                            "bucket",
                            "status",
                            "BucketStatus"
                    );
                    return null;
                },
                -3
        );
    }
}
