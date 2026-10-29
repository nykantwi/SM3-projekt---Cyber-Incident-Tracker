package app.entities;

import app.enums.Role;
import app.security.ISecurityUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;

@Entity
@Table (name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class User implements ISecurityUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;

        // Gemmer password som hash
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @Override
    public boolean verifyPassword(String password) {
        // Brugere uden et password-hash kan ikke logge ind.
        if (password == null || passwordHash == null) {
            return false;
        }

        return BCrypt.checkpw(password, passwordHash);
    }

    // Gemmer kun passwordets hash.
    @Column(name = "password_hash")
    @ToString.Exclude
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private String passwordHash;

    // Hver bruger har én rolle. Rollenavnet gemmes som tekst i databasen.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.REPORTER;
}
