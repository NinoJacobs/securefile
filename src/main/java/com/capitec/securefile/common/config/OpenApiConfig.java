package com.capitec.securefile.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    static final String AUTH_TAG = "Auth";
    static final String ADMIN_TAG = "Admin";
    static final String CUSTOMER_TAG = "Customer";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .tags(List.of(
                        new Tag().name(AUTH_TAG),
                        new Tag().name(ADMIN_TAG),
                        new Tag().name(CUSTOMER_TAG)));
    }
}
