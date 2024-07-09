package edu.stanford.slac.core_work_management.config;

import edu.stanford.slac.core_work_management.model.Counter;
import edu.stanford.slac.core_work_management.model.Work;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuration for the database
 */
@Configuration
@AllArgsConstructor
@EnableMongoRepositories(basePackages = "edu.stanford.slac.core_work_management.repository")
public class ConfigDatabase {
    private final MongoTemplate mongoTemplate;
}

