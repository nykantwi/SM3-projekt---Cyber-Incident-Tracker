    package app;

    import app.config.HibernateConfig;
    import app.dao.IncidentDAO;
    import app.entities.Incident;
    import app.enums.IncidentStatus;
    import app.enums.Severity;
    import jakarta.persistence.EntityManagerFactory;

    import java.util.List;
    import java.util.concurrent.*;



    public class IncidentStatisticsDemo {
        public static void main(String[] args) {

            ExecutorService executor = Executors.newFixedThreadPool(2);

            EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
            IncidentDAO incidentDAO = new IncidentDAO(emf);

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

            Callable<Integer> countMedium = () -> {
                int i = 0;
                for (Incident incident : incidents) {
                    if (incident.getStatus() == IncidentStatus.OPEN
                            && incident.getSeverity() == Severity.MEDIUM) {
                        i++;
                    }
                }

                return i;
            };

            // Tester med flere Incidents for at se om det tæller op eller bliver det samme
            Incident  incident1 = new Incident();
            incident1.setSeverity(Severity.MEDIUM);
            incidents.add(incident1);


            // Sender begge opgaver til thread, så de kan køre samtidigt.
            Future<Integer> openResult = executor.submit(countOpen);
            Future<Integer> openResult1 = executor.submit(countMedium);



            try {
                int openCount = openResult.get();
                int openCount1 = openResult1.get();

                System.out.println("Åbne Open Cases:  " + openCount);
                System.out.println("Åben Case med Medium Severity: " + openCount1);

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
