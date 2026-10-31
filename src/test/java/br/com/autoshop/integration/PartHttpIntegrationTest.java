package br.com.autoshop.integration;

import br.com.autoshop.repository.PartRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartHttpIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartRepository partRepository;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final List<String> codes = new ArrayList<>();
    private String token;

    @BeforeEach
    void createToken() {
        var claims = JwtClaimsSet.builder().subject("part-http-test")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(120)).build();
        token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @AfterEach
    void removeTestParts() {
        // Includes inactive records, which are hidden from the regular service queries.
        partRepository.findAll().stream().filter(part -> codes.contains(part.getCode()))
                .forEach(partRepository::delete);
    }

    @Test
    void administrativeRoutesRequireAuthentication() throws Exception {
        assertEquals(401, send("GET", "/part", null, null).statusCode());
        assertEquals(401, send("POST", "/part/1/stock/entries", "{\"quantity\":1}", null).statusCode());
        assertEquals(401, send("GET", "/part", null, "invalid-token").statusCode());
        assertEquals(200, send("GET", "/part", null, token).statusCode());
    }

    @Test
    void jsonCrudAndStockFlowPreservesReadOnlyFields() throws Exception {
        String code = newCode();
        var created = send("POST", "/part", payload(code, "LITER", "Original")
                .replace("{", "{\"id\":-1,\"stockQuantity\":999,"), token);
        assertEquals(201, created.statusCode(), created.body());
        String path = partPath(created);
        var initial = objectMapper.readTree(send("GET", path, null, token).body());
        assertTrue(initial.get("id").asLong() > 0);
        assertEquals(0, initial.get("stockQuantity").decimalValue().signum());
        assertEquals(200, send("POST", path + "/stock/entries", "{\"quantity\":2.750}", token).statusCode());
        assertEquals(200, send("POST", path + "/stock/exits", "{\"quantity\":0.250}", token).statusCode());
        assertEquals(200, send("PUT", path, payload(code, "LITER", "Updated"), token).statusCode());
        assertEquals(200, send("PATCH", path, "{\"stockQuantity\":999,\"description\":\"Updated\"}", token).statusCode());
        var result = objectMapper.readTree(send("GET", path, null, token).body());
        assertEquals("Updated", result.get("name").asText());
        assertEquals(0, new BigDecimal("2.5").compareTo(result.get("stockQuantity").decimalValue()));
        assertEquals(200, send("DELETE", path, null, token).statusCode());
        assertEquals(404, send("GET", path, null, token).statusCode());
        assertEquals(404, send("PUT", path, payload(code, "LITER", "Updated"), token).statusCode());
        assertEquals(404, send("PATCH", path, "{\"name\":\"Updated\"}", token).statusCode());
        assertEquals(404, send("DELETE", path, null, token).statusCode());
        assertEquals(404, send("POST", path + "/stock/entries", "{\"quantity\":1}", token).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"quantity\":null}", "{\"quantity\":0}", "{\"quantity\":-1}",
            "{\"quantity\":0.0001}", "{\"quantity\":10000000000000000}", "{\"quantity\":\"abc\"}", "{"})
    void rejectsInvalidStockJson(String json) throws Exception {
        String path = createPart("LITER");
        assertEquals(400, send("POST", path + "/stock/entries", json, token).statusCode());
        assertEquals(400, send("POST", path + "/stock/exits", json, token).statusCode());
    }

    @Test
    void rejectsUnitChangeWithStockAndRollsBackWholePatch() throws Exception {
        String code = newCode();
        String path = partPath(send("POST", "/part", payload(code, "LITER", "Original"), token));
        assertEquals(200, send("POST", path + "/stock/entries", "{\"quantity\":2}", token).statusCode());
        assertEquals(400, send("PUT", path, payload(code, "UNIT", "Modified"), token).statusCode());
        assertEquals(400, send("PATCH", path, "{\"name\":\"Modified\",\"unit\":\"UNIT\"}", token).statusCode());
        var part = objectMapper.readTree(send("GET", path, null, token).body());
        assertEquals("LITER", part.get("unit").asText());
        assertEquals("Original", part.get("name").asText());
        assertEquals(200, send("POST", path + "/stock/exits", "{\"quantity\":2}", token).statusCode());
        assertEquals(200, send("PATCH", path, "{\"unit\":\"UNIT\"}", token).statusCode());
        assertEquals(200, send("PUT", path, payload(code, "LITER", "Original"), token).statusCode());
    }

    @Test
    void rejectsInvalidCatalogJsonAndPatchValues() throws Exception {
        assertEquals(400, send("POST", "/part", "{}", token).statusCode());
        String path = createPart("UNIT");
        assertEquals(400, send("PUT", path, "{}", token).statusCode());
        assertEquals(400, send("PATCH", path, "{\"name\":\" \"}", token).statusCode());
        assertEquals(400, send("PATCH", path, "{\"salePrice\":-1}", token).statusCode());
        assertEquals(400, send("PATCH", path, "{\"unit\":\"INVALID\"}", token).statusCode());
    }

    @Test
    void concurrentCreatesWithSameCodeReturnCreatedAndConflict() throws Exception {
        String body = payload(newCode(), "UNIT", "Concurrent");
        var request = request("POST", "/part", body, token);
        var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var statuses = List.of(first.get().statusCode(), second.get().statusCode()).stream().sorted().toList();
        assertEquals(List.of(201, 409), statuses);
    }

    private String createPart(String unit) throws Exception {
        return partPath(send("POST", "/part", payload(newCode(), unit, "Original"), token));
    }

    private String newCode() {
        String code = "HTTP-" + UUID.randomUUID();
        codes.add(code);
        return code;
    }

    private String partPath(HttpResponse<String> response) {
        assertEquals(201, response.statusCode(), response.body());
        return URI.create(response.headers().firstValue("Location").orElseThrow()).getPath()
                .substring("/autoshop".length());
    }

    private String payload(String code, String unit, String name) {
        return """
                {"name":"%s","code":"%s","partType":"SUPPLY","unit":"%s","costPrice":1,"salePrice":2}
                """.formatted(name, code, unit);
    }

    private HttpRequest request(String method, String path, String body, String bearer) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/autoshop" + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json");
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpResponse<String> send(String method, String path, String body, String bearer) throws Exception {
        return client.send(request(method, path, body, bearer), HttpResponse.BodyHandlers.ofString());
    }
}
