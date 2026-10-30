package app.dao;

import app.config.HibernateTestConfig;
import app.entities.Comment;
import app.entities.Incident;
import app.entities.User;
import app.enums.IncidentStatus;
import app.enums.Severity;
import app.enums.Role;
import java.util.List;

import app.exceptions.ApiException;
import app.security.ISecurityDAO;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

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
    private ICommentDAO commentDAO;

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
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Incident").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.getTransaction().commit();
        }
        commentDAO = new CommentDAO(emf);
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



    @Test
    void shouldPersistUserAndFindByEmail() {
        User saved = saveUser("reporter@example.com");
        User found = userDAO.findByEmail(saved.getEmail());
        assertNotNull(saved.getId());
        assertEquals(saved.getId(), found.getId());
        assertEquals("Reporter", found.getName());
        assertEquals(Role.REPORTER, found.getRole());
        assertNull(userDAO.findByEmail("missing@example.com"));
    }

    @Test
    void shouldPersistIncidentWithReporterAndDefaults() {
        User reporter = saveUser("reporter@example.com");
        Incident saved = saveIncident(reporter);
        Incident found = incidentDAO.findById(saved.getId());
        assertNotNull(saved.getId());
        assertNotNull(found);
        assertEquals("Phishing", found.getTitle());
        assertEquals("Suspicious email", found.getDescription());
        assertEquals(reporter.getId(), found.getReportedBy().getId());
        assertEquals(IncidentStatus.OPEN, found.getStatus());
        assertEquals(Severity.MEDIUM, found.getSeverity());
    }

    @Test
    void shouldUpdateAndDeleteIncident() {
        Incident saved = saveIncident(saveUser("reporter@example.com"));
        saved.setTitle("Updated incident");
        saved.setSeverity(Severity.HIGH);
        incidentDAO.update(saved);
        Incident found = incidentDAO.findById(saved.getId());
        assertEquals("Updated incident", found.getTitle());
        assertEquals(Severity.HIGH, found.getSeverity());
        incidentDAO.delete(saved.getId());
        assertNull(incidentDAO.findById(saved.getId()));
        assertDoesNotThrow(() -> incidentDAO.delete(saved.getId()));
    }

    @Test
    void shouldListIncidentsAndFilterByReporter() {
        User first = saveUser("first@example.com");
        User second = saveUser("second@example.com");
        Incident a = saveIncident(first);
        Incident b = saveIncident(second);
        Incident c = saveIncident(first);
        assertEquals(List.of(a.getId(), b.getId(), c.getId()),
                incidentDAO.getAll().stream().map(Incident::getId).toList());
        assertEquals(List.of(a.getId(), c.getId()),
                incidentDAO.findByReporterId(first.getId()).stream().map(Incident::getId).toList());
        assertTrue(incidentDAO.findByReporterId(-1L).isEmpty());
    }


    @Test
    void shouldUpdateCommentAndPreserveCreationTime() {
        User user = saveUser("reporter@example.com");
        Comment saved = saveComment(user, saveIncident(user), "Initial");
        var createdAt = commentDAO.findById(saved.getId()).getCreatedAt();
        saved.setContent("Updated");
        commentDAO.update(saved);
        Comment found = commentDAO.findById(saved.getId());
        assertEquals("Updated", found.getContent());
        assertEquals(createdAt, found.getCreatedAt());
    }

    @Test
    void shouldFilterCommentsByIncidentInIdOrder() {
        User user = saveUser("reporter@example.com");
        Incident first = saveIncident(user);
        Incident second = saveIncident(user);
        Comment a = saveComment(user, first, "First");
        saveComment(user, second, "Other incident");
        Comment b = saveComment(user, first, "Second");
        assertEquals(List.of(a.getId(), b.getId()),
                commentDAO.findByIncidentId(first.getId()).stream().map(Comment::getId).toList());
        assertTrue(commentDAO.findByIncidentId(-1L).isEmpty());
    }

    @Test
    void shouldDeleteCommentWithoutDeletingItsRelations() {
        User user = saveUser("reporter@example.com");
        Incident incident = saveIncident(user);
        Comment comment = saveComment(user, incident, "Remove me");
        commentDAO.delete(comment.getId());
        assertNull(commentDAO.findById(comment.getId()));
        assertNotNull(incidentDAO.findById(incident.getId()));
        assertNotNull(userDAO.findByEmail(user.getEmail()));
        assertDoesNotThrow(() -> commentDAO.delete(comment.getId()));
    }

    @Test
    void shouldRollbackInvalidCommentAndAllowNextWrite() {
        User user = saveUser("reporter@example.com");
        Incident incident = saveIncident(user);
        Comment invalid = new Comment();
        invalid.setAuthor(user);
        invalid.setIncident(incident);
        assertThrows(RuntimeException.class, () -> commentDAO.create(invalid));
        assertTrue(commentDAO.findByIncidentId(incident.getId()).isEmpty());
        Comment valid = saveComment(user, incident, "Valid");
        assertNotNull(commentDAO.findById(valid.getId()));
    }

    @Test
    void serviceUpdatePersistsAllFieldsAndPreservesReporter() {
        User reporter = saveUser("reporter@example.com");
        Incident original = saveIncident(reporter);
        IncidentService service = new IncidentService(incidentDAO, userDAO);

        Incident updated = service.updateIncident(original.getId(), "Updated", "New description",
                Severity.HIGH, IncidentStatus.INVESTIGATING);

        Incident found = incidentDAO.findById(original.getId());
        assertEquals(original.getId(), updated.getId());
        assertEquals("Updated", found.getTitle());
        assertEquals("New description", found.getDescription());
        assertEquals(Severity.HIGH, found.getSeverity());
        assertEquals(IncidentStatus.INVESTIGATING, found.getStatus());
        assertEquals(reporter.getId(), found.getReportedBy().getId());
        assertEquals(1, incidentDAO.getAll().size());
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Reporter");
        user.setEmail(email);
        return userDAO.create(user);
    }

    private Incident saveIncident(User reporter) {
        Incident incident = new Incident();
        incident.setTitle("Phishing");
        incident.setDescription("Suspicious email");
        incident.setReportedBy(reporter);
        return incidentDAO.create(incident);
    }

    private Comment saveComment(User author, Incident incident, String content) {
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setIncident(incident);
        comment.setContent(content);
        return commentDAO.create(comment);
    }

    @Test
    void shouldCreateUserAndVerifyLogin() {
        ISecurityDAO securityDAO = new UserDAO(emf);

        User created = securityDAO.createUser(
                "Testbruger",
                "login@test.com",
                "1234"
        );

        User verified = securityDAO.getVerifiedUser(
                "login@test.com",
                "1234"
        );

        assertEquals(created.getId(), verified.getId());
        assertEquals("login@test.com", verified.getEmail());
        assertEquals(Role.REPORTER, verified.getRole());
    }

    @Test
    void shouldRejectWrongPassword() {
        ISecurityDAO securityDAO = new UserDAO(emf);

        securityDAO.createUser(
                "Testbruger",
                "bruger@test.com",
                "1234"
        );

        ApiException exception = assertThrows(
                ApiException.class,
                () -> securityDAO.getVerifiedUser("bruger@test.com", "forkert")
        );

        assertEquals(401, exception.getCode());
        assertEquals("Forkert e-mail eller password", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        ISecurityDAO securityDAO = new UserDAO(emf);

        securityDAO.createUser("Testbruger", "bruger@test.com", "1234");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> securityDAO.createUser(
                        "Anden bruger", "bruger@test.com", "1234"
                )
        );

        assertEquals(409, exception.getCode());
        assertEquals("E-mail er allerede i brug", exception.getMessage());
    }
}
