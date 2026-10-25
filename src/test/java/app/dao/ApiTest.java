package app.dao;

import app.config.ApplicationConfig;
import app.config.HibernateTestConfig;
import app.entities.Incident;
import app.entities.User;
import app.dto.CreateIncidentDTO;
import app.dto.UpdateIncidentDTO;
import app.enums.IncidentStatus;
import app.enums.Severity;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isEmptyString;

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

    // Samme opsætning som i IncidentDAOTest
    @BeforeEach
    void setUp() {
        // Opretter useren, som hændelsen skal tilhøre.
        User user = new User();
        user.setName("Testbruger");
        user.setEmail("api-test@gg.com");

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
    @Test

    //POST
    void postIncidentCreatesAndPersistsIncident() {
        CreateIncidentDTO newIncident = new CreateIncidentDTO();
        newIncident.setTitle("Ny phishinghændelse");
        newIncident.setDescription("En medarbejder har modtaget en mistænkelig e-mail.");
        newIncident.setSeverity(Severity.HIGH);
        newIncident.setReportedEmail("api-test@gg.com");

        // Samme fremgangsmåde som HotelRouteTest: opret og gem ID'et fra svaret.
        int createdId = given()
                .spec(requestSpec)
                .body(newIncident)
        .when()
                .post("/incidents")
        .then()
                .log().ifValidationFails()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", greaterThan(0))
                .body("title", equalTo(newIncident.getTitle()))
                .body("description", equalTo(newIncident.getDescription()))
                .body("severity", equalTo("HIGH"))
                .body("status", equalTo("OPEN"))
                .extract().path("id");

        // Hent hændelsen igen for at kontrollere, at den faktisk er gemt.
        given()
                .spec(requestSpec)
                .pathParam("id", createdId)
        .when()
                .get("/incidents/{id}")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("id", equalTo(createdId))
                .body("title", equalTo(newIncident.getTitle()))
                .body("description", equalTo(newIncident.getDescription()))
                .body("severity", equalTo("HIGH"))
                .body("status", equalTo("OPEN"));
    }

    @Test
    void postIncidentWithBlankTitleReturns400() {
        CreateIncidentDTO newIncident = new CreateIncidentDTO();
        newIncident.setTitle("");
        newIncident.setDescription("Test med en tom titel.");
        newIncident.setSeverity(Severity.HIGH);
        newIncident.setReportedEmail("api-test@gg.com");

        given()
                .spec(requestSpec)
                .body(newIncident)
                .when()
                .post("/incidents")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("message", equalTo("Titel må ikke være tom"));
    }

    @Test
    void putIncidentUpdatesAndPersistsChanges() {
        UpdateIncidentDTO update = new UpdateIncidentDTO();
        update.setTitle("Opdateret phishinghændelse");
        update.setDescription("Hændelsen undersøges.");
        update.setSeverity(Severity.HIGH);
        update.setStatus(IncidentStatus.INVESTIGATING);

        // Opdater hændelsen, som setUp() har oprettet.
        given()
                .spec(requestSpec)
                .pathParam("id", incidentId)
                .body(update)
        .when()
                .put("/incidents/{id}")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(incidentId.intValue()))
                .body("title", equalTo(update.getTitle()))
                .body("description", equalTo(update.getDescription()))
                .body("severity", equalTo("HIGH"))
                .body("status", equalTo("INVESTIGATING"));

        // Hent hændelsen igen for at kontrollere, at ændringerne er gemt.
        given()
                .spec(requestSpec)
                .pathParam("id", incidentId)
        .when()
                .get("/incidents/{id}")
        .then()
                .statusCode(200)
                .body("title", equalTo(update.getTitle()))
                .body("description", equalTo(update.getDescription()))
                .body("severity", equalTo("HIGH"))
                .body("status", equalTo("INVESTIGATING"));
    }

    @Test
    void deleteIncidentRemovesPersistedIncident() {
        // Slet hændelsen fra setUp(), og kontrollér at svaret er tomt.
        given()
                .spec(requestSpec)
                .pathParam("id", incidentId)
        .when()
                .delete("/incidents/{id}")
        .then()
                .statusCode(204)
                .body(isEmptyString());

        // Hændelsen skal ikke længere kunne hentes efter sletningen.
        given()
                .spec(requestSpec)
                .pathParam("id", incidentId)
        .when()
                .get("/incidents/{id}")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", equalTo("Incident med id " + incidentId + " findes ikke"));
    }

    // Kører én gang, når alle tests er færdige.
    @AfterAll
    static void stopServer() {
        if (app != null) {
            ApplicationConfig.stopServer(app);
        }

        if (emf != null) {
            emf.close();
        }
    }

}
