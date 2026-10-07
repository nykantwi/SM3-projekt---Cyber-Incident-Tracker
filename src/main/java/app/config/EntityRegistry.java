package app.config;

import app.entities.Comment;
import app.entities.Incident;
import app.entities.User;
import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {
        configuration.addAnnotatedClass(Incident.class);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Comment.class);

    }
}
