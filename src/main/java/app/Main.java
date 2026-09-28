package app;

import app.config.HibernateConfig;
import app.dao.IncidentDAO;
import app.dao.UserDAO;
import app.entities.Incident;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf =
                HibernateConfig.getEntityManagerFactory();

        try {
            IncidentDAO incidentDAO = new IncidentDAO(emf);
            UserDAO userDAO = new UserDAO(emf);

            // 1. Genbrug brugeren, eller opret den ved første kørsel.
            User savedUser = userDAO.findByEmail("nana@example.com");

            if (savedUser == null) {
                User user = new User();
                user.setName("Nana");
                user.setEmail("nanayawtnrn@gmail.com");
                savedUser = userDAO.create(user);
            }

            // 2. Opret hændelsen og tilknyt den gemte bruger.
            Incident incident = new Incident();
            incident.setTitle("Test incident");
            incident.setDescription("Testing DAO methods.");
            incident.setReportedBy(savedUser);

            // 3. Gem og hent hændelsen igen fra databasen.
            Incident saved = incidentDAO.create(incident);
            Incident found = incidentDAO.findById(saved.getId());
            System.out.println("Gemt hændelse: " + found);
            System.out.println("Rapportør: " + found.getReportedBy().getName());
            System.out.println("E-mail: " + found.getReportedBy().getEmail());
        } finally {
            emf.close();
        }
    }
}
