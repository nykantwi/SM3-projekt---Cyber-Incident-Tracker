package app.controllers;

import app.dao.IncidentService;
import app.dto.IncidentDTO;
import app.entities.Incident;

import javax.naming.Context;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IncidentController {
    private IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }


    public void getAll(Context ctx) {
        // henter alle hændelser fra databasen og gemmer listen i variablen
        List<Incident> incidents = incidentService.getAllIncidents();

        // Omdan hver hændelse til en DTO.
        List<IncidentDTO> incidentDTOs = new ArrayList<>();

        // Send DTO-listen som JSON med status 200 (OK).
        ctx.status(200).json(incidentDTOs);

        // Omdan hver hændelse til en DTO, og tilføj den til listen.
        for (Incident incident : incidents) {
            incidentDTOs.add(new IncidentDTO(incident));
        }

    }
}
