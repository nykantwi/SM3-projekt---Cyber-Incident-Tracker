package app.dao;

import app.entities.Incident;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class IncidentDAO implements IIncidentDAO{

    private final EntityManagerFactory emf;

    @Override
    public Incident create(Incident incident) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                em.persist(incident);
                em.getTransaction().commit();
                return incident;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public Incident findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Incident.class, id);
        }
    }

    @Override
    public List<Incident> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                    "SELECT i FROM Incident i ORDER BY i.id",
                    Incident.class
            ).getResultList();
        }
    }

    @Override
    public Incident update(Incident incident) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                Incident updatedIncident = em.merge(incident);
                em.getTransaction().commit();
                return updatedIncident;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                Incident incident = em.find(Incident.class, id);

                if (incident != null) {
                    em.remove(incident);
                }

                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public List<Incident> findByReporterId(Long userId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT i FROM Incident i " +
                                    "WHERE i.reportedBy.id = :userId ORDER BY i.id",
                            Incident.class
                    )
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }
}