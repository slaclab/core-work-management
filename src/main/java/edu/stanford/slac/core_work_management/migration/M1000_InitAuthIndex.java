package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthenticationTokenIndex;
import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@AllArgsConstructor
@ChangeUnit(id = "init-auth-index", order = "1000", author = "bisegni")
public class M1000_InitAuthIndex {
    private final MongoTemplate mongoTemplate;
    @Execution
    public void changeSet() {
        // update authentication token index
        InitAuthenticationTokenIndex
                .builder()
                .mongoTemplate(mongoTemplate)
                .build().changeSet();
        // update authorization token index
        InitAuthorizationIndex
                .builder()
                .mongoTemplate(mongoTemplate)
                .build()
                .updateIndex();
    }

    @RollbackExecution
    public void rollback() {

    }
}
