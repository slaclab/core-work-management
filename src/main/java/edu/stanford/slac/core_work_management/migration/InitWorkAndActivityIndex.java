package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.model.Work;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@AllArgsConstructor
@ChangeUnit(id = "init-work-activity-index", order = "3", author = "bisegni")
public class InitWorkAndActivityIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        initWorkIndex();
        initActivityIndex();
    }

    /**
     * This method creates the index for the work collection
     */
    private void initWorkIndex() {
//        MongoDDLOps.createIndex(
//                Work.class,
//                mongoTemplate,
//                new Index().on(
//                                "title",
//                                Sort.Direction.ASC
//                        )
//                        .named("title")
//                        .sparse()
//        );
//        MongoDDLOps.createIndex(
//                Work.class,
//                mongoTemplate,
//                new Index().on(
//                                "description",
//                                Sort.Direction.ASC
//                        )
//                        .named("description")
//                        .sparse()
//        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index().on(
                                "relatedToWorkId",
                                Sort.Direction.ASC
                        )
                        .named("relatedToWorkId")
                        .sparse()
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("title")
                        .onField("description")
                        .build()
        );
    }

    /**
     * This method creates the index for the activity collection
     */
    private void initActivityIndex() {
//        MongoDDLOps.createIndex(
//                Activity.class,
//                mongoTemplate,
//                new Index().on(
//                                "title",
//                                Sort.Direction.ASC
//                        )
//                        .named("title")
//                        .sparse()
//        );
//        MongoDDLOps.createIndex(
//                Activity.class,
//                mongoTemplate,
//                new Index().on(
//                                "description",
//                                Sort.Direction.ASC
//                        )
//                        .named("description")
//                        .sparse()
//        );
        MongoDDLOps.createIndex(
                Activity.class,
                mongoTemplate,
                new Index().on(
                                "activityTypeId",
                                Sort.Direction.ASC
                        )
                        .named("activityTypeId")
                        .sparse()
        );
        MongoDDLOps.createIndex(
                Activity.class,
                mongoTemplate,
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("title")
                        .onField("description")
                        .build()
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
