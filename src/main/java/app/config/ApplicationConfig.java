package app.config;

import app.controllers.ExceptionController;
import app.dao.IncidentDAO;
import app.dao.IncidentService;
import app.dao.UserDAO;
import app.exceptions.ApiException;
import app.routes.Routes;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import jakarta.persistence.EntityManagerFactory;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ApplicationConfig {

    private static Routes routes;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final ExceptionController exceptionController = new ExceptionController();

    public static void configuration(JavalinConfig config) {
        config.router.contextPath = "/api/v1"; // base path for all routes
        config.showJavalinBanner = false;
        config.http.defaultContentType = "application/json"; // default content type for requests
        config.router.apiBuilder(routes.getRoutes());

        // Plugins
        config.bundledPlugins.enableRouteOverview("/routes"); // enables route overview at /routes
    }

    public static String getProperty(String propName) throws IOException
    {
        try (InputStream is = HibernateConfig.class.getClassLoader().getResourceAsStream("properties-from-pom.properties"))
        {
            Properties prop = new Properties();
            prop.load(is);
            return prop.getProperty(propName);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("Could not read property from pom file. Build Maven!");
        }
    }

    // Main bruger denne metode til den almindelige database.
    public static Javalin startServer(int port) {
        return startServer(port, HibernateConfig.getEntityManagerFactory());
    }

    // Testen kan sende sin egen database-factory med.
    public static Javalin startServer(int port, EntityManagerFactory emf) {
        IncidentDAO incidentDAO = new IncidentDAO(emf);
        UserDAO userDAO = new UserDAO(emf);

        // Giv DAO'erne til servicen og servicen videre til routes.
        IncidentService incidentService = new IncidentService(incidentDAO, userDAO);
        routes = new Routes(incidentService);

        var app = Javalin.create(ApplicationConfig::configuration);
        app.exception(ApiException.class, exceptionController::apiExceptionHandler);
        app.exception(Exception.class, exceptionController::exceptionHandler);
        app.start(port);
        return app;
    }

    public static void stopServer(Javalin app) {
        app.stop();
    }
}
