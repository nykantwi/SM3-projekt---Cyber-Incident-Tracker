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

    public void read(Context ctx) {
        // Tjekker at ID'et er et tal større end 0.
        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        // Hent hændelsen med det angivne ID.
        Incident incident = incidentService.getIncidentById(id);

        // Omdan hændelsen til en DTO.
        IncidentDTO incidentDTO = new IncidentDTO(incident);

        // Send hændelsen som JSON.
        ctx.status(200).json(incidentDTO);
    }

    public void create(Context ctx) {
        // Læs JSON fra requesten.
        CreateIncidentDTO jsonRequest =
                ctx.bodyAsClass(CreateIncidentDTO.class);

        // Servicen validerer og gemmer hændelsen.
        Incident incident = incidentService.createIncident(
                jsonRequest.getTitle(),
                jsonRequest.getDescription(),
                jsonRequest.getSeverity(),
                jsonRequest.getReportedEmail()
        );

        // Omdan den gemte hændelse til svarets DTO.
        IncidentDTO incidentDTO = new IncidentDTO(incident);
        ctx.status(201).json(incidentDTO);
    }

    public void update(Context ctx) {
        // Læs og validér ID'et fra URL'en.
        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        UpdateIncidentDTO jsonRequest = ctx.bodyAsClass(UpdateIncidentDTO.class);

        // Servicen validerer og gemmer de nye værdier.
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
        // Tjek, at ID'et er et heltal større end 0.
        Long id = ctx.pathParamAsClass("id", Long.class)
                .check(value -> value > 0, "ID skal være større end 0")
                .get();

        // Slet hændelsen gennem service-laget.
        incidentService.deleteIncident(id);

        // Sletningen lykkedes; svaret har ingen body.
        ctx.status(204);
    }
}
