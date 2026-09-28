package app.dao;

import app.entities.Incident;

import java.util.List;

public interface IIncidentDAO {

    Incident create (Incident incident);

    Incident findById(Long id);

    List<Incident> getAll();

    Incident update (Incident incident);

    void delete (Long id);

    List <Incident> findByReporterId(Long userId);
}
