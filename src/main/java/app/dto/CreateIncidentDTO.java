package app.dto;

import app.enums.Severity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString

public class CreateIncidentDTO {
    private String title;
    private String description;
    private Severity severity;
    private String reportedEmail;


}
