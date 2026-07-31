package org.skarvex.auth.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final ConfigurationManager config;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(ConfigurationManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void connect() {
        HikariConfig hikari = new HikariConfig();

        hikari.setJdbcUrl(config.getString("database.url"));
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setUsername(config.getString("database.username"));
        hikari.setPassword(config.getString("database.password"));

        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);

        this.dataSource = new HikariDataSource(hikari);

        try {
            Flyway flyway = Flyway.configure(getClass().getClassLoader())
                    .dataSource(this.dataSource).baselineOnMigrate(true)
                    .load();

            flyway.migrate();
            logger.info("[Flyway] Database migrations applied successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database: ", e);
        }
    }

    public void disconnect() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("[HikariCP] Database disconnected");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

}
