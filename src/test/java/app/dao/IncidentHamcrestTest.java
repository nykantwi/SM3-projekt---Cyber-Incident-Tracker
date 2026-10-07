package app.dao;

import app.entities.Incident;
import app.entities.User;
import app.enums.IncidentStatus;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IncidentHamcrestTest {
    @Test
    public void IncidentTest(){
        Incident incident = new Incident();
        Incident incident1 = new Incident();

        User user = new User();
        User user1 = new User();

        incident.setDescription("HammerTest");
        incident.setTitle("Hammer Test");
        incident.setReportedBy(user);
        incident.setStatus(IncidentStatus.OPEN);

        user.setName("Nunez");
        user.setEmail("nunez@yahoo.com");

        assertThat(incident.getTitle(),equalTo("Hammer Test"));
        assertThat(incident.getReportedBy(),equalTo(user));
        assertThat(incident.getReportedBy(),equalToObject(user));
        assertThat(incident.getStatus(),equalTo(IncidentStatus.OPEN));






    }
}
