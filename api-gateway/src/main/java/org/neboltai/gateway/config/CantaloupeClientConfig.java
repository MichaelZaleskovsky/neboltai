package org.neboltai.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class CantaloupeClientConfig {
    @Bean(destroyMethod = "dispose")
    ConnectionProvider cantaloupeConnections() {
        return ConnectionProvider.builder("cantaloupe-crop")
                .maxConnections(2)
                .pendingAcquireMaxCount(128)
                .pendingAcquireTimeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    WebClient cantaloupeClient(WebClient.Builder builder,
            @Qualifier("cantaloupeConnections") ConnectionProvider connections,
            @Value("${app.cantaloupe-base-url}") String baseUrl) {
        return builder.clone().baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(connections)))
                .build();
    }
}
