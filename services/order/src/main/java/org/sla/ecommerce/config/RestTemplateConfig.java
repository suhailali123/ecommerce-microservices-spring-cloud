package org.sla.ecommerce.config;

import org.sla.ecommerce.security.TokenRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            TokenRelay.currentToken()
                    .ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}