package org.skarvex.auth.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.skarvex.auth.core.config.ConfigurationProvider;
import org.skarvex.auth.core.database.DatabaseProvider;

import javax.sql.DataSource;
import org.slf4j.Logger;

public class JdbcDatabaseProvider implements DatabaseProvider {

    private final String PATH = "database.jdbc";
    private final ConfigurationProvider config;
    private final Logger logger;
    private HikariDataSource dataSource;

    public JdbcDatabaseProvider(ConfigurationProvider config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void connect() {

        if (isConnected()) return;

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(
                String.format("jdbc:mysql://%s:%d/%s",
                        config.getString(PATH + ".host"),
                        config.getInt(PATH + ".port"),
                        config.getString(PATH + ".database")
                )
        );

        hikariConfig.setUsername(config.getString(PATH + ".username"));
        hikariConfig.setPassword(config.getString(PATH + ".password"));

        hikariConfig.setMaximumPoolSize(config.getInt(PATH + ".pool.max-size"));
        hikariConfig.setMinimumIdle(config.getInt(PATH + ".pool.min-idle"));

        HikariDataSource newDataSource = new HikariDataSource(hikariConfig);

        try {
            Flyway.configure(getClass().getClassLoader())
                    .dataSource(this.dataSource).baselineOnMigrate(true)
                    .load().migrate();

            this.dataSource = newDataSource;

            logger.info("[Flyway] Database migrations applied successfully!");
            logger.info("[HikariCP] Database connected successfully!");
        } catch (Exception e) {
            newDataSource.close();
            throw new RuntimeException(
                    "Failed to initialize database" + e
            );
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("[HikariCP] Database disconnected");
        }
    }

    @Override
    public boolean isConnected() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }

    public DataSource getDataSource() {
        if (!isConnected()) {
            throw new IllegalStateException("Database is not connected");
        }

        return dataSource;
    }
}
