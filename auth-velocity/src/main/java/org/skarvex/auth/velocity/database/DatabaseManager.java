package org.skarvex.auth.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid VARCHAR(36) PRIMARY KEY,
                    password_hash VARCHAR(255) NOT NULL,
                    registration_ip VARCHAR(255) NOT NULL,
                    last_login_ip VARCHAR(255) NOT NULL,
                    auto_login BOOLEAN NOT NULL DEFAULT TRUE
                    email VARCHAR(255) DEFAULT NULL,
                    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    UNIQUE KEY uk_email (email)
                )
            """);

            logger.info("[HikariCP] Database connected");
        } catch (SQLException e) {
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
