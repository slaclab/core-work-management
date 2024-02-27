package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.config.SecurityAuditorAware;
import edu.stanford.slac.core_work_management.model.ActivityType;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

@Repository
@AllArgsConstructor
public class ActivityTypeRepositoryImpl implements ActivityTypeRepositoryCustom {
    MongoTemplate mongoTemplate;
    SecurityAuditorAware securityAuditorAware;
    @Override
    public String ensureActivityType(String workTypeId, ActivityType activityType) {
        String normalizedTitle = normalizeStringWithReplace(
                activityType.getTitle(),
                "",
                ""
        );
        Query query = new Query(
                Criteria.where("title").is(normalizedTitle)
        );
        Update update = new Update()
                .setOnInsert("workTypeId", workTypeId)
                .setOnInsert("title", normalizedTitle)
                .setOnInsert("description", activityType.getDescription())
                .setOnInsert("createdBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("lastModifiedBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("createdDate", LocalDateTime.now())
                .setOnInsert("lastModifiedDate", LocalDateTime.now());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

        ActivityType activityTypeCreated = mongoTemplate.findAndModify(
                query,
                update,
                options,
                ActivityType.class
        );
        return Objects.requireNonNull(activityTypeCreated).getId();
    }
}
