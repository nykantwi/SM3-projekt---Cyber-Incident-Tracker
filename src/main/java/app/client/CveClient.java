package app.client;

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
}