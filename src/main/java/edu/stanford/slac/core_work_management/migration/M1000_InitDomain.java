package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.service.DomainService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@AllArgsConstructor
@Profile({"init-domain", "init-default-data"})
@ChangeUnit(id = "init-domain", order = "1000", author = "bisegni")
public class M1000_InitDomain {
    private final DomainService domainService;

    @Execution
    public void changeSet() {
        String domainId = wrapCatch(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("TEC")
                                .description("TEC")
                                .build()
                ),
                -1
        );

        log.info("Created domain with id: {}", domainId);
    }

    @RollbackExecution
    public void rollback() {

    }
}