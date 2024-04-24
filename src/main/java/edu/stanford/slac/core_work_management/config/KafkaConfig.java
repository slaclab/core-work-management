package edu.stanford.slac.core_work_management.config;

import edu.stanford.slac.core_work_management.elog_api.dto.ImportEntryDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@AutoConfigureBefore(KafkaAutoConfiguration.class)
public class KafkaConfig {
    private final MeterRegistry meterRegistry;
    private final KafkaProperties kafkaProperties;


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