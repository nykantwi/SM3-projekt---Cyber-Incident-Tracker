package app.security;

import app.entities.User;

public interface ISecurityDAO {

    // Find brugeren, og kontrollér passwordet ved login.
    User getVerifiedUser(String email, String password);

    // Opret en bruger med et hashet password og rollen REPORTER.
    User createUser(String name, String email, String password);
}