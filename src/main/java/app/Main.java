package app;

import app.config.HibernateConfig;
import app.dao.IncidentDAO;
import app.entities.Incident;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf =
                HibernateConfig.getEntityManagerFactory();

        try {
            IncidentDAO incidentDAO = new IncidentDAO(emf);

            Incident incident = new Incident();
            incident.setTitle("Test incident");
            incident.setDescription("Testing DAO methods.");

            Incident saved = incidentDAO.create(incident);
            Long id = saved.getId();
            System.out.println("Oprettet: " + saved);

            Incident found = incidentDAO.findById(id);
            System.out.println("Hentet: " + found);

            found.setTitle("Updated test incident");
            incidentDAO.update(found);
            System.out.println("Opdateret: " + incidentDAO.findById(id));

            System.out.println("Alle hændelser:");
            incidentDAO.getAll().forEach(System.out::println);

            incidentDAO.delete(id);
            System.out.println("Efter sletning: " + incidentDAO.findById(id));
        } finally {
            emf.close();
        }
    }
}