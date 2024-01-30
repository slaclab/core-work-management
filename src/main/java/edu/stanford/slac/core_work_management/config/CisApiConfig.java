package edu.stanford.slac.core_work_management.config;

import edu.stanford.slac.core_work_management.cis_api.invoker.ApiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Configuration
public class CisApiConfig {
    @Bean
    public ApiClient apiClient(RestTemplate serviceRestTemplate) {
        ApiClient newCisApiClient = new ApiClient(serviceRestTemplate);
        //TODO needs to be parametrized
        newCisApiClient.setBasePath("http://localhost:8080/api/v1");
        return newCisApiClient;
    }
}
