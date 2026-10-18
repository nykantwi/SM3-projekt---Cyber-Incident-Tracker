package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import app.dao.IncidentService;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {
    private final IncidentRoute incidentRoute;

    public Routes(IncidentService incidentService) {

        // Giv servicen videre til hændelsernes routes.
        this.incidentRoute = new IncidentRoute(incidentService);
    }

    public EndpointGroup getRoutes() {

        return () -> {
            // Saml hændelsernes endpoints under /incidents.
            path("/incidents", incidentRoute.getRoutes());
        };
    }

}
