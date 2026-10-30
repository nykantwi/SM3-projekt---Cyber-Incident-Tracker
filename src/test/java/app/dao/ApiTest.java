package app.dao;

import app.config.ApplicationConfig;
import app.config.HibernateTestConfig;
import app.dto.LoginDTO;
import app.entities.Incident;
import app.entities.User;
import app.dto.CreateIncidentDTO;
import app.dto.UpdateIncidentDTO;
import app.enums.IncidentStatus;
import app.enums.Role;
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
    private String reporterToken;
    private String adminToken;

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("incident_api_test")
                    .withUsername("test")
                    .withPassword("test");


    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.createEntityManagerFactory(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        app = ApplicationConfig.startServer(0, emf);

        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(app.port())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    @BeforeEach
    void setUp() {
        User user = new User("Testbruger", "bruger@test.com", "1234");

        User admin = new User("Admin", "admin@test.com", "1234");
        admin.setRole(Role.ADMIN);

        Incident incident = new Incident();
        incident.setTitle("Test af phishing");
        incident.setReportedBy(user);

        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Incident").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.persist(user);
            em.persist(admin);
            em.persist(incident);
            em.getTransaction().commit();


            incidentId = incident.getId();
        }
        LoginDTO login = new LoginDTO();
        login.setEmail("bruger@test.com");
        login.setPassword("1234");

        reporterToken = given()
                .spec(requestSpec)
                .body(login)
                .when()
                .post("/auth/login")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().path("token");

        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setEmail("admin@test.com");
        adminLogin.setPassword("1234");

        // Logger ind som ADMIN, og gemmertoken til DELETE.
        adminToken = given()
                .spec(requestSpec)
                .body(adminLogin)
        .when()
                .post("/auth/login")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().path("token");
    }


    @Test
    void getAllIncidentsReturns200() {
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
        .when()
                .get("/incidents")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void getAllIncidentsReturnsTestIncident() {
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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
                .header("Authorization", "Bearer " + reporterToken)
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
        newIncident.setReportedEmail("bruger@test.com");


        int createdId = given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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


        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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
        newIncident.setReportedEmail("bruger@test.com");

        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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


        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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

        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
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
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("id", incidentId)
        .when()
                .delete("/incidents/{id}")
        .then()
                .statusCode(204)
                .body(isEmptyString());

        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
                .pathParam("id", incidentId)
        .when()
                .get("/incidents/{id}")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", equalTo("Incident med id " + incidentId + " findes ikke"));
    }


    @Test
    void getIncidentsWithoutTokenReturns401() {
        given()
                .spec(requestSpec)
        .when()
                .get("/incidents")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", equalTo("Du skal være logget ind"));
    }

    @Test
    void getIncidentsWithInvalidTokenReturns401() {
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer ugyldigt-token")
        .when()
                .get("/incidents")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", equalTo("Token er ugyldigt"));
    }

    @Test
    void reporterCannotDeleteIncident() {
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
                .pathParam("id", incidentId)
        .when()
                .delete("/incidents/{id}")
        .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("message", equalTo("Kun ADMIN må slette hændelser"));

        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + reporterToken)
                .pathParam("id", incidentId)
        .when()
                .get("/incidents/{id}")
        .then()
                .statusCode(200)
                .body("id", equalTo(incidentId.intValue()));
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        LoginDTO login = new LoginDTO();
        login.setEmail("bruger@test.com");
        login.setPassword("forkert-password");

        given()
                .spec(requestSpec)
                .body(login)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", equalTo("Forkert e-mail eller password"));
    }

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
