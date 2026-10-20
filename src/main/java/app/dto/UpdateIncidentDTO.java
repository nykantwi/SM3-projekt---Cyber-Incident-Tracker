package app.dto;

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

public class UpdateIncidentDTO {
    private String title;
    private String description;
    private Severity severity;
    private IncidentStatus status;


}
