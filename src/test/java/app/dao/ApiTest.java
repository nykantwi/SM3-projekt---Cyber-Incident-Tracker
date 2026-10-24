package app.dao;

import app.config.ApplicationConfig;
import app.config.HibernateTestConfig;
import app.entities.Incident;
import app.entities.User;
import io.javalin.Javalin;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Testcontainers
public class ApiTest {
    private static EntityManagerFactory emf;
    private static Javalin app;
    private static RequestSpecification requestSpec;
    private Long incidentId;

    // Starter en separat database til API-testene.
    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("incident_api_test")
                    .withUsername("test")
                    .withPassword("test");

    // Kører én gang før alle tests.
    @BeforeAll
    static void startServer() {
        // Forbind til den separate testdatabase.
        emf = HibernateTestConfig.createEntityManagerFactory(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // Start serveren med testdatabasen på en ledig port.
        app = ApplicationConfig.startServer(0, emf);

        // Fælles adresse og JSON-format til vores HTTP-tests.
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(app.port())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    // Samme opsætning som i IncidentDAOTest: friske data før hver test.
    @BeforeEach
    void setUp() {
        // Opret rapportøren, som hændelsen skal tilhøre.
        User user = new User();
        user.setName("Testbruger");
        user.setEmail("api-test@example.com");

        Incident incident = new Incident();
        incident.setTitle("Test af phishing");
        incident.setReportedBy(user);

        // Status er automatisk OPEN og severity MEDIUM.

        // Gem testdata i den separate testdatabase.
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Ryd kun testdatabasen, med kommentarer før hændelser og brugere.
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Incident").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.persist(user);
            em.persist(incident);
            em.getTransaction().commit();

            // Gem ID’et efter hændelsen er gemt i databasen.
            incidentId = incident.getId();
        }
    }

    @Test
    void getAllIncidentsReturns200() {
        given()
                .spec(requestSpec) // Brug vores fælles request-indstillinger.
        .when()
                .get("/incidents") // Send en GET-request.
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void getAllIncidentsReturnsTestIncident() {
        given()
                .spec(requestSpec)
        .when()
                .get("/incidents")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("title", hasItem("Test af phishing"));
    }

    @Test
    void getIncidentByIdReturnsTestIncident() {
        given()
                .spec(requestSpec)
                .pathParam("id", incidentId)
        .when()
                .get("/incidents/{id}")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(incidentId.intValue()))
                .body("title", equalTo("Test af phishing"))
                .body("status", equalTo("OPEN"));
    }
    // Kører én gang, når alle tests er færdige.
    @AfterAll
    static void stopServer() {
        // Stop serveren, før databaseforbindelserne lukkes.
        if (app != null) {
            ApplicationConfig.stopServer(app);
        }

        if (emf != null) {
            emf.close(); // create-drop fjerner testtabellerne.
        }
    }

}
