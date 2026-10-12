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

    // Genbrug klienten til API-kald.
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String fetchJson(String cveId)
            throws IOException, InterruptedException {

        // Tjek formatet, fx CVE-2021-44228.
        if (cveId == null || !cveId.matches("CVE-\\d{4}-\\d{4,}")) {
            throw new IllegalArgumentException("Ugyldigt CVE-ID");
        }

        URI uri = URI.create(
                "https://services.nvd.nist.gov/rest/json/cves/2.0?cveId="
                        + cveId
        );

        // Byg et GET-kald med en tidsgrænse.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        // Send kaldet, og modtag JSON som tekst.
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Stop, hvis API'et ikke svarer med HTTP 200 (OK).
        if (response.statusCode() != 200) {
            throw new IOException(
                    "NVD svarede med HTTP " + response.statusCode()
            );
        }

        return response.body();
    }

    public CveDTO fetchById(String cveId)
            throws IOException, InterruptedException {

        // fetchJson klarer validering og HTTP-kald.
        String json = fetchJson(cveId);
        JsonNode root = objectMapper.readTree(json);

        if (root == null || !root.isObject()) {
            throw new IllegalStateException("Ugyldigt svar fra NVD");
        }

        JsonNode results = root.path("vulnerabilities");
        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalStateException("Ingen CVE fundet: " + cveId);
        }

        JsonNode cve = results.get(0).path("cve");

        if (!cve.path("id").isTextual() || !cveId.equals(cve.path("id").asText())) {
            throw new IllegalStateException("NVD returnerede et ugyldigt CVE-ID");
        }

        CveDTO dto = new CveDTO();
        dto.setCveId(cve.get("id").asText());

        // Beskrivelser kan have flere sprog; vælg den engelske.
        for (JsonNode description : cve.path("descriptions")) {
            if ("en".equals(description.path("lang").asText())
                    && description.path("value").isTextual()) {
                dto.setDescription(description.get("value").asText());
                break;
            }
        }

        dto.setCvssScore(findCvssScore(cve.path("metrics")));
        return dto;
    }

    private Double findCvssScore(JsonNode metrics) {
        // Foretræk nyere CVSS-versioner. Manglende score forbliver null.
        for (String version : new String[]{"cvssMetricV40", "cvssMetricV31",
                "cvssMetricV30", "cvssMetricV2"}) {
            Double fallback = null;
            for (JsonNode metric : metrics.path(version)) {
                JsonNode score = metric.path("cvssData").path("baseScore");
                if (!score.isNumber() || score.asDouble() < 0 || score.asDouble() > 10) {
                    continue;
                }
                // Brug den primære vurdering, hvis den findes.
                if ("Primary".equals(metric.path("type").asText())) {
                    return score.asDouble();
                }
                if (fallback == null) {
                    fallback = score.asDouble();
                }
            }
            if (fallback != null) {
                return fallback;
            }
        }
        return null;
    }
}
