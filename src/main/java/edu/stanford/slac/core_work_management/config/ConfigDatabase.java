package edu.stanford.slac.core_work_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuration for the database
 */
@Configuration
@EnableMongoRepositories(basePackages = "edu.stanford.slac.core_work_management.repository")
public class ConfigDatabase {

}

