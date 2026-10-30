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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    private Severity severity = Severity.MEDIUM;

    @ManyToOne
    @JoinColumn(name = "reported_by_id", nullable = false)
    @ToString.Exclude
    private User reportedBy;
}