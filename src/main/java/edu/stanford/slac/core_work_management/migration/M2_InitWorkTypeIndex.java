package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.WorkType;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-work-type-index", order = "2", author = "bisegni")
public class M2_InitWorkTypeIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        initWorkTypeIndex();
    }

    /**
     * This method creates the index for the work collection
     */
    private void initWorkTypeIndex() {
        MongoDDLOps.createIndex(
                WorkType.class,
                mongoTemplate,
                new Index().on(
                                "title",
                                Sort.Direction.ASC
                        )
                        .named("title")
                        .unique()
        );
    }


    @RollbackExecution
    public void rollback() {

    }
}
