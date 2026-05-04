package com.caioamorimr.ordermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management API")
                        .version("1.0.0")
                        .description("REST API para gestão de pedidos, produtos, categorias e usuários.")
                        .contact(new Contact()
                                .name("Caio Amorim")
                                .email("caioamorimribeiro@gmail.com")
                                .url("https://github.com/caioamorimr")));
    }
}