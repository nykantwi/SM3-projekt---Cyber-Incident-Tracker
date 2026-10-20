package app.routes;

import app.controllers.IncidentController;
import app.dao.IncidentService;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;


public class IncidentRoute {

    private final IncidentController incidentController;

    public IncidentRoute(IncidentService incidentService) {
        // Giv controlleren den service, den skal bruge.
        this.incidentController = new IncidentController(incidentService);
    }

    protected EndpointGroup getRoutes() {
        return () -> {
            // Forbind GET på gruppens rodsti med getAll-metoden.
            get("/", incidentController::getAll);
            get("/{id}",incidentController::read);
            post("/", incidentController::create);
            put("/{id}", incidentController::update);
            delete("/{id}", incidentController::delete);
        };
    }
}
