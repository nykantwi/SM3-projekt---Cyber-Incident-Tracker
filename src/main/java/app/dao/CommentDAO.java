package app.dao;

import app.entities.Comment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CommentDAO implements ICommentDAO{

    private final EntityManagerFactory emf;

    @Override
    public Comment create(Comment comment) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                em.persist(comment);
                em.getTransaction().commit();
                return comment;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public Comment findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Comment.class, id);
        }
    }

    @Override
    public Comment update(Comment comment) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                Comment updatedComment = em.merge(comment);
                em.getTransaction().commit();
                return updatedComment;
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
                Comment comment = em.find(Comment.class, id);

                if (comment != null) {
                    em.remove(comment);
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
    public List<Comment> findByIncidentId(Long incidentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT i FROM Comment i " +
                                    "WHERE i.incident.id = :incidentId ORDER BY i.id",
                            Comment.class
                    )
                    .setParameter("incidentId", incidentId)
                    .getResultList();
        }
    }
}
