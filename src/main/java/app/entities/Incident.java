package app.entities;

import app.enums.IncidentStatus;
import app.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // Gemmer enum-navnet, fx "OPEN", som tekst i databasen.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    // Nye Java-objekter starter med status OPEN.
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    // Nye Java-objekter starter med alvorlighedsgraden MEDIUM.
    private Severity severity = Severity.MEDIUM;
}