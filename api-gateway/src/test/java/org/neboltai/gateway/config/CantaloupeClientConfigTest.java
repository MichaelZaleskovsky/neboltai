package org.neboltai.gateway.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CantaloupeClientConfigTest {
    @Test
    void queuesConcurrentCropRequestsAndConsumesAllResponses() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        var executor = Executors.newCachedThreadPool();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/image", exchange -> {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                Thread.sleep(100);
                exchange.sendResponseHeaders(200, 2);
                try (var output = exchange.getResponseBody()) { output.write(new byte[]{1, 2}); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
                exchange.close();
            }
        });
        server.start();
        var config = new CantaloupeClientConfig();
        var connections = config.cantaloupeConnections();
        try {
            var client = config.cantaloupeClient(WebClient.builder(), connections,
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var responses = Flux.range(0, 16).flatMap(index -> client.get().uri("/image")
                    .retrieve().bodyToMono(byte[].class), 16).collectList().block(Duration.ofSeconds(15));
            assertNotNull(responses);
            assertEquals(16, responses.size());
            responses.forEach(body -> assertArrayEquals(new byte[]{1, 2}, body));
            assertEquals(2, peak.get());
        } finally {
            connections.dispose();
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
