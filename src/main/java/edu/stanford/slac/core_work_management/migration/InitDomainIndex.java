package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthenticationTokenIndex;
import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.LOVElement;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-domain-index", order = "5", author = "bisegni")
public class InitDomainIndex {
    private final MongoTemplate mongoTemplate;
    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Domain.class,
                mongoTemplate,
                new Index().on(
                                "name",
                                Sort.Direction.ASC
                        )
                        .named("name")
                        .unique()
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
