package edu.stanford.slac.core_work_management.repository;

import com.mongodb.DuplicateKeyException;
import edu.stanford.slac.core_work_management.config.SecurityAuditorAware;
import edu.stanford.slac.core_work_management.model.ActivityType;
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
}
