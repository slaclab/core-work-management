package edu.stanford.slac.core_work_management.config;

import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConf {
    @Value("${springdoc.title}")
    private String apiTitle;

    @Value("${springdoc.description}")
    private String apiDescription;

    @Value("${springdoc.contact.name}")
    private String contactName;

    @Value("${springdoc.contact.url}")
    private String contactUrl;

    @Value("${springdoc.contact.email}")
    private String contactEmail;

    @Autowired
    private AppProperties appProperties;
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title(apiTitle)
                        .description(apiDescription)
                        .contact(new Contact().name(contactName).url(contactUrl).email(contactEmail)))
//                .servers(
//                        servers.stream()
//                                .map(server -> new Server().url(server.get("url")).description(server.get("description")))
//                                .collect(Collectors.toList())
//                )
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(appProperties.getUserHeaderName())
                                .bearerFormat("JWT")))
                ;
        // ... (More configurations)
    }
}
