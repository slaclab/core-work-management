package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.LOVElement;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-lov-index", order = "4", author = "bisegni")
public class M4_InitLOVIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                LOVElement.class,
                mongoTemplate,
                new Index().on(
                                "value",
                                Sort.Direction.ASC
                        )
                        .on(
                                "groupName",
                                Sort.Direction.ASC
                        )
                        .named("value-group-name")
                        .unique()
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
