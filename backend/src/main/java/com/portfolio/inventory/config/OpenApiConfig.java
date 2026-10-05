package com.portfolio.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI inventoryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Field Asset Tracker API")
                .version("v1")
                .description("Portfolio project with synthetic data. Errors use RFC 9457 problem details.")
                .license(new License().name("MIT")));
    }
}
