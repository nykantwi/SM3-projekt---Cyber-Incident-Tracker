package app.controllers;

import app.dao.IncidentService;
import app.dto.IncidentDTO;
import app.entities.Incident;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;

public class IncidentController {
    private IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        // Gem servicen til brug i controllerens metoder.
        this.incidentService = incidentService;
    }


    public void getAll(Context ctx) {
        // Hent hændelserne gennem service-laget.
        List<Incident> incidents = incidentService.getAllIncidents();

        // Opret listen til de DTO'er, klienten skal modtage.
        List<IncidentDTO> incidentDTOs = new ArrayList<>();

        // Omdan hver hændelse til en DTO, og tilføj den til listen.
        for (Incident incident : incidents) {
            incidentDTOs.add(new IncidentDTO(incident));
        }
        // Send den udfyldte liste som JSON med status 200 (OK).
        ctx.status(200).json(incidentDTOs);

    }
}
