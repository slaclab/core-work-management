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
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "createdDate",
                                Sort.Direction.ASC
                        )
                        .named("created-date")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "workType.id",
                                Sort.Direction.ASC
                        )
                        .named("workType-id")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "createdBy",
                                Sort.Direction.ASC
                        )
                        .named("created-by")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "assignedTo",
                                Sort.Direction.ASC
                        )
                        .named("assigned-to")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "workType.workflow.name",
                                Sort.Direction.ASC
                        )
                        .named("workType-name")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "currentStatus.status",
                                Sort.Direction.ASC
                        )
                        .named("currentStatus-status")
        );
        MongoDDLOps.createIndex(
                Work.class,
                mongoTemplate,
                new Index()
                        .on(
                                "currentBucketAssociation.bucketId",
                                Sort.Direction.ASC
                        )
                        .named("currentBucketAssociation.bucketId")
                        .sparse()
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
