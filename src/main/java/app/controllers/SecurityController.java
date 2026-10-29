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

    // Modtag DAO'en, ligesom IncidentController modtager sin service.
    public SecurityController(ISecurityDAO securityDAO) {
        this.securityDAO = securityDAO;
    }

    public void register(Context ctx) {
        // Læs navn, e-mail og password fra JSON-requesten.
        CreateUserDTO request = ctx.bodyAsClass(CreateUserDTO.class);

        // DAO'en validerer oplysningerne og gemmer brugeren.
        securityDAO.createUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        // Bekræft oprettelsen uden at sende password eller hash tilbage.
        ctx.status(201).json(new Message(201, "Bruger oprettet"));
    }
    private String createToken(User user) {
        // Læs serverens hemmelige signeringsnøgle fra miljøvariabler.
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET mangler");
        }

        // Biblioteket forventer en samling af roller. Vi sender kun brugerens ene rolle.
        UserDTO tokenUser = new UserDTO(
                user.getEmail(),
                Set.of(user.getRole().name())
        );

        try {
            // Tokenet udstedes af CIT og gælder i én time (millisekunder).
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
        // Læs e-mail og password fra requesten.
        LoginDTO request = ctx.bodyAsClass(LoginDTO.class);

        // Tjek, at brugeren findes, og passwordet passer.
        User user = securityDAO.getVerifiedUser(
                request.getEmail(),
                request.getPassword()
        );

        // Opret et JWT til brugeren.
        String token = createToken(user);

        // Send tokenet tilbage som JSON.
        ctx.status(200).json(new TokenDTO(token));
    }

    public void authenticate(Context ctx) {
        // Hent Authorization-headeren fra requesten.
        String header = ctx.header("Authorization");

        // Afvis requesten, hvis der ikke er sendt et Bearer-token.
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Du skal være logget ind");
        }

        // Fjern "Bearer ", så vi kun har selve tokenet.
        String token = header.substring(7);

        // Hent samme nøgle, som vi brugte til at oprette tokenet.
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET mangler");
        }

        try {
            // Kontrollér signaturen og tokenets udløbstid.
            if (!tokenSecurity.tokenIsValid(token, secret)
                    || !tokenSecurity.tokenNotExpired(token)) {
                throw new ApiException(401, "Token er ugyldigt eller udløbet");
            }

            // Gem brugerens oplysninger til resten af denne request.
            UserDTO user = tokenSecurity.getUserWithRolesFromToken(token);
            ctx.attribute("user", user);

        } catch (ParseException | TokenVerificationException e) {
            throw new ApiException(401, "Token er ugyldigt");
        }

    }
    public void authorizeAdmin(Context ctx) {
        // Hent brugeren, som authenticate har gemt på requesten.
        UserDTO user = ctx.attribute("user");

        if (user == null) {
            throw new ApiException(401, "Du skal være logget ind");
        }

        // Kun ADMIN må fortsætte.
        if (!user.getRoles().contains("ADMIN")) {
            throw new ApiException(403, "Kun ADMIN må slette hændelser");
        }
    }
}