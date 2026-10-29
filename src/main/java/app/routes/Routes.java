package app.routes;

import app.security.ISecurityDAO;
import io.javalin.apibuilder.EndpointGroup;
import app.dao.IncidentService;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {
    private final IncidentRoute incidentRoute;
    private final SecurityRoute securityRoute;

    public Routes(IncidentService incidentService, ISecurityDAO securityDAO) {

        // Giv servicen videre til hændelsernes routes.
        this.incidentRoute = new IncidentRoute(incidentService);
        this.securityRoute = new SecurityRoute(securityDAO);
    }

    public EndpointGroup getRoutes() {

        return () -> {
            // Saml hændelsernes endpoints under /incidents.
            path("/incidents", incidentRoute.getRoutes());
            path("/auth",securityRoute.getRoutes());
        };
    }

}
