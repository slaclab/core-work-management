package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.UUID;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

@Repository
@AllArgsConstructor
public class ActivityTypeRepositoryImpl implements ActivityTypeRepositoryCustom {
    MongoTemplate mongoTemplate;
    @Override
    public String ensureActivityType(ActivityType activityType) {
        String normalizedActivityTypeName = normalizeStringWithReplace(
                activityType.getName(),
                " ",
                "-"
        );

        Query query = new Query(
                Criteria.where("name").is(normalizedActivityTypeName)
        );
        Update update = new Update()
                .setOnInsert("id", UUID.randomUUID().toString())
                .setOnInsert("name", normalizedActivityTypeName);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

        ActivityType activity = mongoTemplate.findAndModify(
                query,
                update,
                options,
                ActivityType.class
        );
        return Objects.requireNonNull(activity).getId();
    }
}
