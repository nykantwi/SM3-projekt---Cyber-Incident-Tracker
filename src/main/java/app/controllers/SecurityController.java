package app.controllers;

import app.dto.LoginDTO;
import app.dto.TokenDTO;
import app.security.ISecurityDAO;
import app.dto.CreateUserDTO;
import app.exceptions.Message;
import io.javalin.http.Context;
import app.entities.User;
import app.exceptions.ApiException;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.TokenCreationException;
import dk.bugelhartmann.UserDTO;
import java.text.ParseException;
import dk.bugelhartmann.TokenVerificationException;

import java.util.Set;

public class SecurityController {

    private final ISecurityDAO securityDAO;
    private final TokenSecurity tokenSecurity = new TokenSecurity();

    public SecurityController(ISecurityDAO securityDAO) {
        this.securityDAO = securityDAO;
    }

    public void register(Context ctx) {
        CreateUserDTO request = ctx.bodyAsClass(CreateUserDTO.class);

        securityDAO.createUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        ctx.status(201).json(new Message(201, "Bruger oprettet"));
    }
    private String createToken(User user) {
        // Læs serverens signeringsnøgle
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET mangler");
        }

        UserDTO tokenUser = new UserDTO(
                user.getEmail(),
                Set.of(user.getRole().name())
        );

        try {
            return tokenSecurity.createToken(
                    tokenUser,
                    "CIT",
                    "3600000",
                    secret
            );
        } catch (TokenCreationException e) {
            throw new ApiException(500, "Kunne ikke oprette token");
        }
    }

    public void login(Context ctx) {
        LoginDTO request = ctx.bodyAsClass(LoginDTO.class);

        User user = securityDAO.getVerifiedUser(
                request.getEmail(),
                request.getPassword()
        );

        String token = createToken(user);

        ctx.status(200).json(new TokenDTO(token));
    }

    public void authenticate(Context ctx) {
        String header = ctx.header("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Du skal være logget ind");
        }

        String token = header.substring(7);

        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET mangler");
        }

        try {
            if (!tokenSecurity.tokenIsValid(token, secret)
                    || !tokenSecurity.tokenNotExpired(token)) {
                throw new ApiException(401, "Token er ugyldigt eller udløbet");
            }

            UserDTO user = tokenSecurity.getUserWithRolesFromToken(token);
            ctx.attribute("user", user);

        } catch (ParseException | TokenVerificationException e) {
            throw new ApiException(401, "Token er ugyldigt");
        }

    }
    public void authorizeAdmin(Context ctx) {
        UserDTO user = ctx.attribute("user");

        if (user == null) {
            throw new ApiException(401, "Du skal være logget ind");
        }

        if (!user.getRoles().contains("ADMIN")) {
            throw new ApiException(403, "Kun ADMIN må slette hændelser");
        }
    }
}