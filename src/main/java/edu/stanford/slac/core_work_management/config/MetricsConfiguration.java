package edu.stanford.slac.core_work_management.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MetricsConfiguration {
        private final MeterRegistry meterRegistry;
}
