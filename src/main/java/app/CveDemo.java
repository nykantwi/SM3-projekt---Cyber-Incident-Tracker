package app;

import app.client.CveClient;
import java.io.IOException;

public class CveDemo {

    public static void main(String[] args) {
        CveClient client = new CveClient();

        try {
            // Hent og vis API'ets rå JSON-svar.
            String json = client.fetchJson("CVE-2021-44228");
            System.out.println(json);

        } catch (IOException e) {
            System.err.println("API-kaldet fejlede: " + e.getMessage());

        } catch (InterruptedException e) {
            // Bevar signalet om, at tråden blev afbrudt.
            Thread.currentThread().interrupt();
            System.err.println("API-kaldet blev afbrudt.");
        }
    }
}