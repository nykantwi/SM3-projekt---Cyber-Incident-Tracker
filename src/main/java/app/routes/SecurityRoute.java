package app.routes;

import app.controllers.SecurityController;
import app.security.ISecurityDAO;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.post;

public class SecurityRoute {
    private final SecurityController securityController;

    public SecurityRoute(ISecurityDAO securityDAO) {
        // Giv DAO'en videre til controlleren.
        this.securityController = new SecurityController(securityDAO);
    }

    protected EndpointGroup getRoutes(){
        return () -> {
            post("/register", securityController::register);
            post("/login",securityController::login);

        };

    }
}
