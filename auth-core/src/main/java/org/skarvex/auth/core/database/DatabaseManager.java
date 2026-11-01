package org.skarvex.auth.core.database;

import org.skarvex.auth.core.config.ConfigurationProvider;
import org.skarvex.auth.core.database.provider.JdbcDatabaseProvider;
import org.skarvex.auth.core.database.provider.MongoDatabaseProvider;
import org.skarvex.auth.core.database.repository.RestrictedIpRepository;
import org.skarvex.auth.core.database.repository.UserRepository;
import org.skarvex.auth.core.database.repository.jdbc.JdbcRestrictedIpRepository;
import org.skarvex.auth.core.database.repository.jdbc.JdbcUserRepository;

import org.skarvex.auth.core.database.repository.mongodb.MongoRestrictedIpRepository;
import org.skarvex.auth.core.database.repository.mongodb.MongoUserRepository;
import org.slf4j.Logger;

public class DatabaseManager {

    private final DatabaseProvider provider;
    private final UserRepository userRepository;
    private final RestrictedIpRepository restrictedIpRepository;

    public DatabaseManager(
            ConfigurationProvider config,
            Logger logger
    ) {
        DatabaseType type = DatabaseType.valueOf(
                config.getString("database.type").toUpperCase()
        );

        switch (type) {
            case MYSQL -> {
                JdbcDatabaseProvider jdbc = new JdbcDatabaseProvider(config, logger);

                this.provider = jdbc;
                this.userRepository = new JdbcUserRepository(jdbc.getDataSource());
                this.restrictedIpRepository = new JdbcRestrictedIpRepository(jdbc.getDataSource());
            } case MONGODB -> {
                MongoDatabaseProvider mongo = new MongoDatabaseProvider(config, logger);

                this.provider = mongo;
                this.userRepository = new MongoUserRepository(mongo.getDatabase());
                this.restrictedIpRepository = new MongoRestrictedIpRepository(mongo.getDatabase());
            } default -> throw new IllegalStateException("Unknown database type: " + type);
        }
    }

    public void connect() {
        this.provider.connect();
    }

    public void disconnect() {
        this.provider.disconnect();
    }

    public boolean isConnected() {
        return this.provider.isConnected();
    }
}
