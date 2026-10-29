package app.security;

import app.enums.Role;

public interface ISecurityUser {
    Role getRole();

    boolean verifyPassword(String password);

    void setRole(Role role);
}
