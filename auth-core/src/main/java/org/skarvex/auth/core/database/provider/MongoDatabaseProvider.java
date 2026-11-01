package org.skarvex.auth.core.database.provider;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.skarvex.auth.core.config.ConfigurationProvider;
import org.skarvex.auth.core.database.DatabaseProvider;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class MongoDatabaseProvider implements DatabaseProvider {

    private static final String PATH = "database.mongodb";

    private final ConfigurationProvider config;
    private final Logger logger;

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDatabaseProvider(ConfigurationProvider config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void connect() {
        if (isConnected()) return;

        String host = config.getString(PATH + ".host");
        int port = config.getInt(PATH + ".port");
        String databaseName = config.getString(PATH + ".database");

        // Credentials
        String username = config.getString(PATH + ".username");
        String password = config.getString(PATH + ".password");
        String authDatabase = config.getString(PATH + ".authentication-database");

        MongoCredential credential = MongoCredential.createCredential(
                username, authDatabase, password.toCharArray()
        );

        // Connection
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(
                        new ConnectionString(
                                String.format("mongodb://%s:%d", host, port)
                        )
                ).credential(credential)
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10, TimeUnit.SECONDS)
                ).build();

        MongoClient newClient = MongoClients.create(settings);

        try {
            MongoDatabase db = newClient.getDatabase(databaseName);
            db.runCommand(
                    new org.bson.Document("ping", 1)
            );

            this.mongoClient = newClient;
            this.database = db;

            logger.info("[MongoDB] Connected successfully!");
        } catch (Exception e) {
            newClient.close();

            throw new RuntimeException(
                    "Failed to connect to MongoDB", e
            );
        }
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            logger.info("[MongoDB] Disconnected");
        }
    }

    @Override
    public boolean isConnected() {
        return mongoClient != null;
    }

    public MongoDatabase getDatabase() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "MongoDB is not connected"
            );
        }

        return database;
    }
}
