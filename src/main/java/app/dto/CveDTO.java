package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CveDTO {

    private String cveId;
    private String description;

    private Double cvssScore;
}
