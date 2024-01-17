package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.MongoDDLOps;
import edu.stanford.slac.core_work_management.model.Location;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@AllArgsConstructor
@ChangeUnit(id = "init-location-index", order = "1", author = "bisegni")
public class InitLocationIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Location.class,
                mongoTemplate,
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("name")
                        .onField("description")
                        .build()
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
