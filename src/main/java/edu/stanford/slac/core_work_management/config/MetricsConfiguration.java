package edu.stanford.slac.core_work_management.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MetricsConfiguration {
    private final MeterRegistry meterRegistry;
    @Bean
    public Counter previewProcessedCounter() {
        return Counter
                .builder("cwm_preview_processing_event")
                .tag("operation", "processing")
                .tag("state", "success")
                .description("The number of preview processed successfully")
                .register(meterRegistry);
    }

    @Bean
    public Counter previewErrorsCounter() {
        return Counter
                .builder("cwm_preview_processing_event")
                .tag("operation", "processing")
                .tag("state", "failed")
                .description("The number of preview processed with errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter previewSubmittedCounter() {
        return Counter
                .builder("cwm_preview_processing_event")
                .tag("operation", "submission")
                .tag("state", "success")
                .description("The number of preview request submitted")
                .register(meterRegistry);
    }

    @Bean
    public Counter previewRetrySubmitted() {
        return Counter
                .builder("cwm_preview_retry_event")
                .tag("operation", "retry")
                .tag("state", "failed")
                .description("The number of retry resubmission")
                .register(meterRegistry);
    }
}
