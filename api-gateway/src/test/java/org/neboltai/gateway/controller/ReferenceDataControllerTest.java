package org.neboltai.gateway.controller;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.neboltai.gateway.ApiGatewayApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import io.swagger.v3.core.util.Json;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceDataControllerTest {
    @Test
    void documentsAuthorFieldsAndForwardsNamedFieldsWithCreatedStatus() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        HttpServer backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backend.createContext("/api/addAuthor", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":42,\"author\":\"Vasiliev Vasia Vasilich\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        backend.start();
        try (var app = new SpringApplicationBuilder(ApiGatewayApplication.class)
                .web(WebApplicationType.REACTIVE)
                .run("--server.port=0", "--app.artwork-service-url=http://127.0.0.1:" + backend.getAddress().getPort())) {
            String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
            try (HttpClient client = HttpClient.newHttpClient()) {
                var docs = client.send(HttpRequest.newBuilder(URI.create(url + "/v3/api-docs")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, docs.statusCode());
                var json = Json.mapper().readTree(docs.body());
                var schema = json.at("/components/schemas/AddAuthorRequest");
                assertTrue(schema.path("properties").has("familyName"));
                assertTrue(schema.path("properties").has("name"));
                assertTrue(schema.path("properties").has("secondName"));
                assertTrue(schema.path("properties").has("description"));
                assertEquals("familyName", schema.path("required").get(0).asText());
                assertEquals("Vasiliev", schema.at("/properties/familyName/example").asText());
                assertEquals("#/components/schemas/AddAuthorRequest", json.at(
                        "/paths/~1api~1addAuthor/post/requestBody/content/application~1json/schema/$ref").asText());
                assertTrue(json.at("/paths/~1api~1addAuthor/post/responses").has("201"));
                assertTrue(json.at("/components/schemas/AddMediaRequest/properties").has("typeId"));
                assertTrue(json.at("/components/schemas/AddMediaTypeRequest/properties").has("type"));

                String request = "{\"familyName\":\"Vasiliev\",\"name\":\"Vasia\",\"secondName\":\"Vasilich\"}";
                var result = client.send(HttpRequest.newBuilder(URI.create(url + "/api/addAuthor"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(request)).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(201, result.statusCode());
                assertEquals(42, Json.mapper().readTree(result.body()).path("id").asLong());
                var forwarded = Json.mapper().readTree(received.get());
                assertEquals("Vasiliev", forwarded.path("familyName").asText());
                assertEquals("Vasia", forwarded.path("name").asText());
                assertEquals("Vasilich", forwarded.path("secondName").asText());
            }
        } finally {
            backend.stop(0);
        }
    }
}
