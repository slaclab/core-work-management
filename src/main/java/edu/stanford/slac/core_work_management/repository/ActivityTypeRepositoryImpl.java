package edu.stanford.slac.core_work_management.repository;

import com.mongodb.DuplicateKeyException;
import edu.stanford.slac.core_work_management.config.SecurityAuditorAware;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.ActivityTypeCustomField;
import edu.stanford.slac.core_work_management.model.WorkType;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

@Repository
@AllArgsConstructor
public class ActivityTypeRepositoryImpl implements ActivityTypeRepositoryCustom {
    MongoTemplate mongoTemplate;
    SecurityAuditorAware securityAuditorAware;

    @Override
    public String ensureActivityType(ActivityType activityType) {
        ActivityType activityTypeCreated = null;
        String normalizedTitle = normalizeStringWithReplace(
                activityType.getTitle(),
                "",
                ""
        );
        Query query = new Query(
                Criteria.where("title").is(normalizedTitle)
        );
        Update update = new Update()
                .setOnInsert("title", normalizedTitle)
                .setOnInsert("description", activityType.getDescription())
                .setOnInsert("createdBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("lastModifiedBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("createdDate", LocalDateTime.now())
                .setOnInsert("lastModifiedDate", LocalDateTime.now());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        try {
            activityTypeCreated = mongoTemplate.findAndModify(
                    query,
                    update,
                    options,
                    ActivityType.class
            );
        } catch (DuplicateKeyException e) {
            // The insert failed because the document already exists, so fetch and return it
            activityTypeCreated = mongoTemplate.findOne(query, ActivityType.class);
        }
        return Objects.requireNonNull(activityTypeCreated).getId();
    }

    @Override
    public Optional<ActivityTypeCustomField> findCustomFiledById(String activityTypeId, String customFieldId) {
        // Query to find the specific ActivityType
        Query query = new Query(
                Criteria.where("_id").is(activityTypeId).and("customFields.id").is(customFieldId)
        );
        // Execute the query
        ActivityType activityType = mongoTemplate.findOne(query, ActivityType.class);
        if (activityType != null && activityType.getCustomFields() != null) {
            // Filter the custom fields to find the one with the matching ID
            return activityType.getCustomFields().stream()
                    .filter(field -> customFieldId.equals(field.getId()))
                    .findFirst();
        }
        return Optional.empty();
    }
}
