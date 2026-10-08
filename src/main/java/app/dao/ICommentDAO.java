package app.dao;

import app.entities.Comment;
import java.util.List;

public interface ICommentDAO {
    Comment create(Comment comment);
    Comment findById(Long id);
    List<Comment> findByIncidentId(Long incidentId);
    Comment update(Comment comment);
    void delete(Long id);
}
