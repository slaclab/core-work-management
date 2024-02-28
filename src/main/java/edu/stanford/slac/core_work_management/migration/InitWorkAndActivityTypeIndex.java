package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@AllArgsConstructor
@ChangeUnit(id = "init-work-activity-type-index", order = "2", author = "bisegni")
public class InitWorkAndActivityTypeIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        initWorkTypeIndex();
        initActivityTypeIndex();
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

    /**
     * This method creates the index for the activity collection
     */
    private void initActivityTypeIndex() {
        MongoDDLOps.createIndex(
                ActivityType.class,
                mongoTemplate,
                new Index().on(
                                "title",
                                Sort.Direction.ASC
                        )
                        .on(
                                "workTypeId",
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
