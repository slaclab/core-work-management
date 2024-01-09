package edu.stanford.slac.core_work_management.repository;

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
        String normalizedActivityTypeName = normalizeStringWithReplace(
                workType.getTitle(),
                " ",
                "-"
        );

        Query query = new Query(
                Criteria.where("name").is(normalizedActivityTypeName)
        );
        Update update = new Update()
                .setOnInsert("id", UUID.randomUUID().toString())
                .setOnInsert("name", normalizedActivityTypeName)
                .setOnInsert("createdBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("lastModifiedBy", securityAuditorAware.getCurrentAuditor().orElse(null))
                .setOnInsert("createdDate", LocalDateTime.now())
                .setOnInsert("lastModifiedDate", LocalDateTime.now())
                .setOnInsert("version", 0);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

        WorkType workTypeCreated = mongoTemplate.findAndModify(
                query,
                update,
                options,
                WorkType.class
        );
        return Objects.requireNonNull(workTypeCreated).getId();
    }
}
