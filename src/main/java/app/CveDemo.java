package app;

import app.client.CveClient;
import app.dto.CveDTO;
import java.io.IOException;

public class CveDemo {

    public static void main(String[] args) {
        CveClient client = new CveClient();

        try {
            // Hent kun de oplysninger, vi bruger i CIT.
            CveDTO cve = client.fetchById("CVE-2021-44228");
            System.out.println("CVE: " + cve.getCveId());
            System.out.println("Beskrivelse: " + cve.getDescription());
            System.out.println("CVSS-score: " + cve.getCvssScore());

        } catch (IOException | IllegalStateException e) {
            System.err.println("API-kaldet fejlede: " + e.getMessage());

        } catch (InterruptedException e) {
            // Bevar signalet om, at tråden blev afbrudt.
            Thread.currentThread().interrupt();
            System.err.println("API-kaldet blev afbrudt.");
        }
    }
}