package app.dto;

import app.entities.Incident;
import app.enums.IncidentStatus;
import app.enums.Severity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString

public class IncidentDTO {
    private Long id;
    private String title;
    private String description;
    private IncidentStatus status;
    private Severity severity;
    private Long reportedById;

    public IncidentDTO(Incident incident) {
        this.id = incident.getId();
        this.title = incident.getTitle();
        this.description = incident.getDescription();
        this.status = incident.getStatus();
        this.severity = incident.getSeverity();
        this.reportedById = incident.getReportedBy().getId();
    }
}
