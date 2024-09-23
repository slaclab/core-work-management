package edu.stanford.slac.core_work_management.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Log4j2
@Getter
@Setter
@ConfigurationProperties(prefix = "edu.stanford.slac.core-work-management")
public class CWMAppProperties {
    private String cisBackendUrl;
    private String elogBackendUrl;
    private String imagePreviewTopic;
    private String workflowProcessingTopic;
    private Integer kafkaConsumerConcurrency;
}
