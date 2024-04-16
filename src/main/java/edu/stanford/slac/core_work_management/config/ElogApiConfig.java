package edu.stanford.slac.core_work_management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.core_work_management.elog_api.api.EntriesControllerApi;
import edu.stanford.slac.core_work_management.elog_api.invoker.ApiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Log4j2
@Configuration
public class ElogApiConfig {
    @Value("${edu.stanford.slac.core_work_management.elog_backend_url}")
    private String elogBackendUrl;
    private final ObjectMapper objectMapper;

    public ElogApiConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public EntriesControllerApi entriesControllerApi(RestTemplate serviceRestTemplate) {
        log.info("Configure ELOG client to URL: {}", elogBackendUrl);
        // Create a message converter with your custom ObjectMapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        // Set the message converter to the RestTemplate
        serviceRestTemplate.setMessageConverters(List.of(converter));
        ApiClient newApiClient = new ApiClient(serviceRestTemplate);
        newApiClient.setBasePath(elogBackendUrl);
        return new EntriesControllerApi(newApiClient);
    }
}
