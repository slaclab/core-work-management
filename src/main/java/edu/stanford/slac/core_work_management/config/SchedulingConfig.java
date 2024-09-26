package edu.stanford.slac.core_work_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile({"async-ops"})
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
