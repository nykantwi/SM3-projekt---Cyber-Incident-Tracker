package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// De CVE-oplysninger, som CIT bruger fra det eksterne API.
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CveDTO {

    private String cveId;
    private String description;

    // Double tillader null, hvis kilden ikke oplyser en score.
    private Double cvssScore;
}
