package app.dao;

import app.config.HibernateTestConfig;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class IncidentDAOTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("incident_test")
                    .withUsername("test")
                    .withPassword("test");

    private static EntityManagerFactory emf;
    private IIncidentDAO incidentDAO;
    private IUserDAO userDAO;

    @BeforeAll
    public static void setUpAll(){
        emf = HibernateTestConfig.createEntityManagerFactory(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    @BeforeEach
    public void setUp(){
        incidentDAO = new IncidentDAO(emf);
        userDAO = new UserDAO(emf);
    }
    
    @AfterAll
    public static void tearDownAll() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void shouldStartTestDatabase(){
        assertTrue(postgres.isRunning());
        assertTrue(emf.isOpen());
    }


}