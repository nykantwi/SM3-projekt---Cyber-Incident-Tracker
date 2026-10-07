package app.dao;

import app.config.HibernateTestConfig;
import app.entities.Comment;
import app.entities.Incident;
import app.entities.User;
import app.enums.IncidentStatus;
import app.enums.Severity;
import app.enums.Role;
import java.util.List;
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
        // Ryd kun den isolerede testdatabase, i rækkefølge efter fremmednøgler.
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
    void shouldPersistCommentWithAuthorIncidentAndTimestamp() {
        User reporter = saveUser("reporter@example.com");
        User author = saveUser("analyst@example.com");
        Incident incident = saveIncident(reporter);
        Comment saved = saveComment(author, incident, "Investigating");
        Comment found = commentDAO.findById(saved.getId());
        assertNotNull(saved.getId());
        assertEquals("Investigating", found.getContent());
        assertNotNull(found.getCreatedAt());
        assertEquals(author.getId(), found.getAuthor().getId());
        assertEquals(incident.getId(), found.getIncident().getId());
        assertEquals(reporter.getId(), found.getIncident().getReportedBy().getId());
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
}
