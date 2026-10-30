package app.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CveClientTest {

    private CveClient clientWith(String json) {
        return new CveClient() {
            @Override
            public String fetchJson(String cveId) {
                return json;
            }
        };
    }

    @Test
    void mapsIdAndEnglishDescription() throws Exception {
        var client = clientWith("""
                {"vulnerabilities":[{"cve":{
                  "id":"CVE-2021-44228",
                  "descriptions":[{"lang":"es","value":"Otro"},{"lang":"en","value":"Log4j"}]
                }}]}
                """);
        var dto = client.fetchById("CVE-2021-44228");
        assertEquals("CVE-2021-44228", dto.getCveId());
        assertEquals("Log4j", dto.getDescription());
    }

    @Test
    void missingDescriptionRemainsNull() throws Exception {
        var dto = clientWith("""
                {"vulnerabilities":[{"cve":{"id":"CVE-2021-44228"}}]}
                """).fetchById("CVE-2021-44228");
        assertNull(dto.getDescription());
    }

    @Test
    void descriptionWithoutEnglishRemainsNull() throws Exception {
        var dto = clientWith("""
                {"vulnerabilities":[{"cve":{"id":"CVE-2021-44228",
                  "descriptions":[{"lang":"es","value":"Otro"}]
                }}]}
                """).fetchById("CVE-2021-44228");
        assertNull(dto.getDescription());
    }

    @Test
    void rejectsMissingResultsAndUnexpectedId() {
        assertThrows(IllegalStateException.class,
                () -> clientWith("{\"vulnerabilities\":[]}").fetchById("CVE-2021-44228"));
        assertThrows(IllegalStateException.class, () -> clientWith("""
                {"vulnerabilities":[{"cve":{"id":"CVE-2020-1234"}}]}
                """).fetchById("CVE-2021-44228"));
    }
}
