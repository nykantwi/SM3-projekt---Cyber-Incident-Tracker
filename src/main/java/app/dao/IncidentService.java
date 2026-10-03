package app.dao;

import app.entities.Incident;
import app.entities.User;
import app.enums.IncidentStatus;
import app.enums.Severity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class IncidentService {
    private final IIncidentDAO incidentDAO;
    private final IUserDAO userDAO;

    public Incident createIncident(String title, String description, Severity severity, String reportedEmail) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title må ikke være tom");
        }

        if (reportedEmail == null || reportedEmail.isBlank()) {
            throw new IllegalArgumentException("Email må ikke være tom");
        }

        if (severity == null) {
            throw new IllegalArgumentException("Severity must be provided");
        }

        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description must be max 2000 characters");
        }
        User reporter = userDAO.findByEmail(reportedEmail);
        if (reporter == null) {
            throw new NoSuchElementException("Brugeren findes ikke");
        }

        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setSeverity(severity);
        incident.setReportedBy(reporter);

        return incidentDAO.create(incident);
    }

    public Incident getIncidentById(Long id) {
        validateId(id);

        Incident incident = incidentDAO.findById(id);
        if (incident == null) {
            throw new NoSuchElementException("Incident med id " + id + " findes ikke");
        }

        return incident;
    }

    public List<Incident> getAllIncidents() {
        return incidentDAO.getAll();
    }

    public List<Incident> getIncidentsByReporter(Long userId) {
        validateId(userId);
        return incidentDAO.findByReporterId(userId);
    }

    public Incident changeStatus(Long incidentId, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status må ikke være null");
        }

        Incident incident = getIncidentById(incidentId);
        incident.setStatus(newStatus);
        return incidentDAO.update(incident);
    }

    public void deleteIncident(Long id) {
        getIncidentById(id);
        incidentDAO.delete(id);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id skal være større end 0");
        }
    }
}
