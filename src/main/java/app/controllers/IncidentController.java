package app.controllers;

import app.dao.IncidentService;
import app.dto.CreateIncidentDTO;
import app.dto.IncidentDTO;
import app.dto.UpdateIncidentDTO;
import app.entities.Incident;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;

public class IncidentController {
    private IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }


    public void getAll(Context ctx) {
        List<Incident> incidents = incidentService.getAllIncidents();
        List<IncidentDTO> incidentDTOs = new ArrayList<>();

        for (Incident incident : incidents) {
            incidentDTOs.add(new IncidentDTO(incident));
        }
        ctx.status(200).json(incidentDTOs);

    }

    public void read(Context ctx) {

        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        Incident incident = incidentService.getIncidentById(id);
        IncidentDTO incidentDTO = new IncidentDTO(incident);


        ctx.status(200).json(incidentDTO);
    }

    public void create(Context ctx) {
        CreateIncidentDTO jsonRequest =
                ctx.bodyAsClass(CreateIncidentDTO.class);

        Incident incident = incidentService.createIncident(
                jsonRequest.getTitle(),
                jsonRequest.getDescription(),
                jsonRequest.getSeverity(),
                jsonRequest.getReportedEmail()
        );

        IncidentDTO incidentDTO = new IncidentDTO(incident);
        ctx.status(201).json(incidentDTO);
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        UpdateIncidentDTO jsonRequest = ctx.bodyAsClass(UpdateIncidentDTO.class);

        Incident incident = incidentService.updateIncident(
                id,
                jsonRequest.getTitle(),
                jsonRequest.getDescription(),
                jsonRequest.getSeverity(),
                jsonRequest.getStatus()
        );

        ctx.status(200).json(new IncidentDTO(incident));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        incidentService.deleteIncident(id);
        ctx.status(204);
    }
}
