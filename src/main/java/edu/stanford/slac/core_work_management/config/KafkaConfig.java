package edu.stanford.slac.core_work_management.config;

import edu.stanford.slac.core_work_management.elog_api.dto.ImportEntryDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

import java.util.Map;

@Log4j2
@Configuration
@RequiredArgsConstructor
@AutoConfigureBefore(KafkaAutoConfiguration.class)
public class KafkaConfig {
    private final MeterRegistry meterRegistry;
    private final KafkaProperties kafkaProperties;

    @Value("${edu.stanford.slac.core-work-management.elog-import-topic}")
    private String importEntryTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        StringBuilder topicDescriptionLog = new StringBuilder();
        Map<String, Object> configs = kafkaProperties.buildAdminProperties();
        var kAdmin = new KafkaAdmin(configs);
        var topicDescription = kAdmin.describeTopics(importEntryTopic);
        if (topicDescription.containsKey(importEntryTopic)) {
            if(topicDescription.get(importEntryTopic).authorizedOperations() != null) {
                topicDescription.get(importEntryTopic).authorizedOperations().forEach(
                        authorizedOperation -> topicDescriptionLog.append("Topic %s has authorized operation %s".formatted(importEntryTopic, authorizedOperation.toString()))
                );
            } else {
                topicDescriptionLog.append("Topic %s has no authorized operations".formatted(importEntryTopic));
            }
        } else {
            topicDescriptionLog.append("Topic %s is not in the broker".formatted(importEntryTopic));
        }
        log.info(topicDescriptionLog.toString());
        return kAdmin;
    }

    @Bean
    public ProducerFactory<String, ImportEntryDTO> importEntryProducerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        DefaultKafkaProducerFactory<String, ImportEntryDTO> pf = new DefaultKafkaProducerFactory<>(props);
        pf.addListener(new MicrometerProducerListener<>(meterRegistry));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, ImportEntryDTO> importEntryDTOKafkaTemplate() {
        return new KafkaTemplate<>(importEntryProducerFactory());
    }
}