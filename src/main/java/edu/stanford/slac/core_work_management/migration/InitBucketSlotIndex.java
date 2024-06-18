package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.LOVElement;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-bucket-slot-index", order = "5", author = "bisegni")
public class InitBucketSlotIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                BucketSlot.class,
                mongoTemplate,
                new Index().on(
                                "createdDate",
                                Sort.Direction.ASC
                        )
                        .on(
                                "from",
                                Sort.Direction.DESC
                        )
                        .named("value-group-name")
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
