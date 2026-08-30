package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI feeCalculatorOpenApi() {
        Contact contact = new Contact()
                .name("Fee Calculator API Team");

        Info info = new Info()
                .title("Fee Calculator API")
                .description("REST API for processing payments, calculating fees, and managing payment records.")
                .version("1.0.0")
                .contact(contact);

        return new OpenAPI().info(info);
    }
}
