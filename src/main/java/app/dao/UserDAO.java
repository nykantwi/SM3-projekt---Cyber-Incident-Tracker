package app.dao;

import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserDAO {
    private final EntityManagerFactory emf;

    public User create(User user){
        try(EntityManager em = emf.createEntityManager()){
            em.getTransaction().begin();

            try {
                em.persist(user);
                em.getTransaction().commit();
                return user;
            } catch (RuntimeException e){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }
    public User findByEmail(String email) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email",
                            User.class
                    )
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
