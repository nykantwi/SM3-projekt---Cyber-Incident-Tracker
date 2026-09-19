package app.config;

import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {
        // Register entity classes here once they have been created.
        // Example: configuration.addAnnotatedClass(YourEntity.class);
    }
}
