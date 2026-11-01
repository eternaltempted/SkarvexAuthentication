package org.skarvex.auth.core.database.repository.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.skarvex.auth.core.model.User;
import org.skarvex.auth.core.database.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class MongoUserRepository implements UserRepository {

    private final MongoCollection<Document> collection;

    public MongoUserRepository(MongoDatabase database) {
        this.collection = database.getCollection("users");
        collection.createIndex(
                Indexes.ascending("uuid"),
                new IndexOptions().unique(true)
        );
    }

    @Override
    public Optional<User> findById(UUID uuid) {
        Document document = collection.find(
                new Document("uuid", uuid.toString())).first();

        return Optional.ofNullable(document).map( doc ->
                new User(
                        UUID.fromString(doc.getString("uuid")),
                        doc.getString("password_hash"),
                        doc.getString("registration_ip"),
                        doc.getString("last_login_ip"),
                        doc.getBoolean("auto_login"),
                        doc.getString("email"),
                        doc.getBoolean("is_email_verified")
                )
        );
    }

    @Override
    public void save(User user) {
        Document document = new Document()
                .append("uuid", user.id().toString())
                .append("password_hash", user.passwordHash())
                .append("registration_ip", user.registrationIp())
                .append("last_login_ip", user.lastLoginIp())
                .append("auto_login", user.autoLogin());

        collection.insertOne(document);
    }

    @Override
    public boolean updatePassword(UUID uuid, String passwordHash) {

        UpdateResult result = collection.updateOne(
                Filters.eq("uuid", uuid.toString()),
                Updates.set("password_hash", passwordHash)
        );

        return result.getMatchedCount() > 0;
    }

    @Override
    public boolean updateLastLoginIp(UUID uuid, String ip) {

        UpdateResult result = collection.updateOne(
                Filters.eq("uuid", uuid.toString()),
                Updates.set("last_login_ip", ip)
        );

        return result.getMatchedCount() > 0;

    }

    @Override
    public boolean setAutoLogin(UUID uuid, boolean enabled) {

        UpdateResult result = collection.updateOne(
                Filters.eq("uuid", uuid.toString()),
                Updates.set("auto_login", enabled)
        );

        return result.getMatchedCount() > 0;
    }

    @Override
    public boolean updateEmail(UUID uuid, String email) {

        UpdateResult result = collection.updateOne(
                Filters.eq("uuid", uuid.toString()),
                Updates.set("email", email)
        );

        return result.getMatchedCount() > 0;
    }

    @Override
    public boolean isEmailLinked(String email) {

        Document document = collection
                .find(Filters.eq("email", email))
                .projection(Projections.include("email"))
                .first();

        return document != null;
    }
}
