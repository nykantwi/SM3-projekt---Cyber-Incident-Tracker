package app;

import app.config.HibernateConfig;
import app.dao.IncidentDAO;
import app.entities.Incident;
import app.enums.IncidentStatus;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.concurrent.*;



public class IncidentStatisticsDemo {
    public static void main(String[] args) {

        // Puljen kan udføre op til to opgaver samtidigt.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Hent factoryen, som DAO'en bruger til databaseforbindelser.
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        IncidentDAO incidentDAO = new IncidentDAO(emf);
        //gemmer all incident i daoen som en List af incident
        List<Incident> incidents = incidentDAO.getAll();


        Callable<Integer> countOpen = () -> {
            int i = 0;
            for (Incident incident : incidents) {
                if (incident.getStatus() == IncidentStatus.OPEN) {
                    i++;
                }
            }

            return i;
        };

        Future<Integer> openResult = executor.submit(countOpen);


        try {
            int openCount = openResult.get();

            System.out.println("Åbne Open Cases:  " + openCount);

        } catch (ExecutionException e) {
            throw new RuntimeException(e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RuntimeException("Ventetiden blev afbrudt", e);

        } finally {
            executor.shutdown();
            emf.close();
        }
    }

}
