package app;

import app.config.ApplicationConfig;

public class Main {

    public static void main(String[] args) {
        // Start REST API'et på port 7070.
        ApplicationConfig.startServer(7070);
    }
}
