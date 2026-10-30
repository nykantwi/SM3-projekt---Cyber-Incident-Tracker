package app.client;

import app.dto.CveDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class CveClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String fetchJson(String cveId)
            throws IOException, InterruptedException {

        if (cveId == null || !cveId.matches("CVE-\\d{4}-\\d{4,}")) {
            throw new IllegalArgumentException("Ugyldigt CVE-ID");
        }

        URI uri = URI.create(
                "https://services.nvd.nist.gov/rest/json/cves/2.0?cveId="
                        + cveId
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "NVD svarede med HTTP " + response.statusCode()
            );
        }

        return response.body();
    }

    public CveDTO fetchById(String cveId)
            throws IOException, InterruptedException {

        String json = fetchJson(cveId);
        // Læs JSON som et træ, så vi kan finde de indlejrede felter.
        JsonNode root = objectMapper.readTree(json);

        if (root == null || !root.isObject()) {
            throw new IllegalStateException("Ugyldigt svar fra NVD");
        }

        JsonNode results = root.path("vulnerabilities");
        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalStateException("Ingen CVE fundet: " + cveId);
        }

        // Find CVE-objektet i det første søgeresultat.
        JsonNode cve = results.get(0).path("cve");

        if (!cve.path("id").isTextual() || !cveId.equals(cve.path("id").asText())) {
            throw new IllegalStateException("NVD returnerede et ugyldigt CVE-ID");
        }

        CveDTO dto = new CveDTO();
        dto.setCveId(cve.get("id").asText());

        // Vælg de engelske beskrivelse til vores DTO.
        for (JsonNode description : cve.path("descriptions")) {
            if ("en".equals(description.path("lang").asText())
                    && description.path("value").isTextual()) {
                dto.setDescription(description.get("value").asText());
                break;
            }
        }

        return dto;
    }


}
