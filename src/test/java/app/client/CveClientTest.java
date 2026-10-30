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
    void mapsEnglishDescriptionAndPrefersPrimaryScore() throws Exception {
        var client = clientWith("""
                {"vulnerabilities":[{"cve":{
                  "id":"CVE-2021-44228",
                  "descriptions":[{"lang":"es","value":"Otro"},{"lang":"en","value":"Log4j"}],
                  "metrics":{"cvssMetricV31":[
                    {"type":"Secondary","cvssData":{"baseScore":9.0}},
                    {"type":"Primary","cvssData":{"baseScore":10.0}}
                  ]}
                }}]}
                """);
        var dto = client.fetchById("CVE-2021-44228");
        assertEquals("CVE-2021-44228", dto.getCveId());
        assertEquals("Log4j", dto.getDescription());
        assertEquals(10.0, dto.getCvssScore());
    }

    @Test
    void missingOptionalDataRemainsNull() throws Exception {
        var dto = clientWith("""
                {"vulnerabilities":[{"cve":{"id":"CVE-2021-44228"}}]}
                """).fetchById("CVE-2021-44228");
        assertNull(dto.getDescription());
        assertNull(dto.getCvssScore());
    }

    @Test
    void fallsBackToOlderScore() throws Exception {
        var dto = clientWith("""
                {"vulnerabilities":[{"cve":{"id":"CVE-2021-44228",
                  "metrics":{"cvssMetricV2":[{"cvssData":{"baseScore":7.5}}]}
                }}]}
                """).fetchById("CVE-2021-44228");
        assertEquals(7.5, dto.getCvssScore());
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
