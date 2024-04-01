package edu.stanford.slac.core_work_management.repository;

import com.mongodb.DuplicateKeyException;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkQueryParameterDTO;
import edu.stanford.slac.core_work_management.config.SecurityAuditorAware;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

@Repository
@AllArgsConstructor
public class WorkTypeRepositoryImpl implements WorkTypeRepositoryCustom {
    MongoTemplate mongoTemplate;
    SecurityAuditorAware securityAuditorAware;

    @Override
    public String ensureWorkType(WorkType workType) {
        WorkType workTypeCreated = null;
        String normalizedTitle = normalizeStringWithReplace(
                workType.getTitle(),
                "",
                ""
        );

        Query query = new Query(
                Criteria.where("title").is(normalizedTitle)
        );
        Update update = new Update()
                .setOnInsert("title", normalizedTitle)
                .setOnInsert("description", workType.getDescription())
                .setOnInsert("createdBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("lastModifiedBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("createdDate", LocalDateTime.now())
                .setOnInsert("lastModifiedDate", LocalDateTime.now());
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        try {
            workTypeCreated = mongoTemplate.findAndModify(
                    query,
                    update,
                    options,
                    WorkType.class
            );
        } catch (DuplicateKeyException e) {
            // The insert failed because the document already exists, so fetch and return it
            workTypeCreated = mongoTemplate.findOne(query, WorkType.class);
        }
        return Objects.requireNonNull(workTypeCreated).getId();
    }


    /**
     * Find the custom field by ID
     * @param workTypeId the activity type ID
     * @param customFieldId the custom field ID
     * @return the custom field
     */
    @Override
    public Optional<WATypeCustomField> findCustomFieldById(String workTypeId, String customFieldId) {
        // Query to find the specific ActivityType
        Query query = new Query(
                Criteria.where("_id").is(workTypeId).and("customFields.id").is(customFieldId)
        );
        // Execute the query
        WorkType activityType = mongoTemplate.findOne(query, WorkType.class);
        if (activityType != null && activityType.getCustomFields() != null) {
            // Filter the custom fields to find the one with the matching ID
            return activityType.getCustomFields().stream()
                    .filter(field -> customFieldId.equals(field.getId()))
                    .findFirst();
        }
        return Optional.empty();
    }
}
