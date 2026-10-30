package app.security;

import app.entities.User;

public interface ISecurityDAO {

    User getVerifiedUser(String email, String password);

    User createUser(String name, String email, String password);
}