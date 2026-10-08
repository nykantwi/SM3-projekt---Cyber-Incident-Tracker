package app.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

public class HibernateTestConfig {
    public static EntityManagerFactory createEntityManagerFactory(
            String jdbcUrl, String username, String password) {

        Properties properties = HibernateBaseProperties.createBase();

        properties.setProperty("hibernate.connection.url", jdbcUrl);
        properties.setProperty("hibernate.connection.username", username);
        properties.setProperty("hibernate.connection.password", password);
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");

        return HibernateEmfBuilder.build(properties);
    }
}
