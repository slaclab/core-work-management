package edu.stanford.slac.core_work_management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.core_work_management.cis_api.api.InventoryElementControllerApi;
import edu.stanford.slac.core_work_management.cis_api.invoker.ApiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Log4j2
@Configuration
public class CisApiConfig {
    @Value("${edu.stanford.slac.core-work-management.cis-backend-url}")
    private String cisBackendUrl;
    private final ObjectMapper objectMapper;

    public CisApiConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public InventoryElementControllerApi inventoryElementControllerApi(RestTemplate serviceRestTemplate) {
        log.info("Configure CIS client to URL: {}", cisBackendUrl);
        // Create a message converter with your custom ObjectMapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        // Set the message converter to the RestTemplate
        serviceRestTemplate.setMessageConverters(List.of(converter));
        ApiClient newApiClient = new ApiClient(serviceRestTemplate);
        newApiClient.setBasePath(cisBackendUrl);
        return new InventoryElementControllerApi(newApiClient);
    }
}
