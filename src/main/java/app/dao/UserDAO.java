package app.dao;

import app.entities.User;
import app.exceptions.ApiException;
import app.security.ISecurityDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor

public class UserDAO implements IUserDAO, ISecurityDAO {
    private final EntityManagerFactory emf;

    @Override
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

    @Override
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

    @Override
    public User getVerifiedUser(String email, String password) {
        // Genbrug vores eksisterende metode til at finde brugeren.
        User user = findByEmail(email);

        // Samme fejlbesked, uanset om e-mail eller password er forkert.
        if (user == null || !user.verifyPassword(password)) {
            throw new ApiException(401, "Forkert e-mail eller password");
        }

        return user;
    }

    @Override
    public User createUser(String name, String email, String password) {
        // Kontrollér, at de nødvendige oplysninger er udfyldt.
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            throw new ApiException(400, "Navn, e-mail og password skal udfyldes");
        }

        if (findByEmail(email) != null) {
            throw new ApiException(409, "E-mail er allerede i brug");
        }

        // Constructoren hasher passwordet. Rollen er automatisk REPORTER.
        User user = new User(name, email, password);

        // Genbrug metoden, som gemmer brugeren med JPA.
        return create(user);
    }
}
