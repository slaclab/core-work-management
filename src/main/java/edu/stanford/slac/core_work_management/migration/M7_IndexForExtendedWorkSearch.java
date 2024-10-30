package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.Work;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-work-extended-search-index", order = "7", author = "bisegni")
public class M7_IndexForExtendedWorkSearch {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "domainId",
                                Sort.Direction.ASC
                        )
                        .named("domain-id")
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}