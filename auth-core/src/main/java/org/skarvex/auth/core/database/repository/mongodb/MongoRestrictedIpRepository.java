package org.skarvex.auth.core.database.repository.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.skarvex.auth.core.model.RestrictedIp;
import org.skarvex.auth.core.database.repository.RestrictedIpRepository;

import java.util.Optional;

public class MongoRestrictedIpRepository implements RestrictedIpRepository {

    private final MongoCollection<Document> collection;

    public MongoRestrictedIpRepository(MongoDatabase database) {
        this.collection = database.getCollection("restricted_ips");
        collection.createIndex(
                Indexes.ascending("ip"),
                new IndexOptions().unique(true)
        );
    }

    @Override
    public void block(RestrictedIp restrictedIp) {
        Document doc = new Document()
                .append("ip", restrictedIp.ip())
                .append("blocked_until", restrictedIp.blockedUntil());

        collection.insertOne(doc);
    }

    @Override
    public Optional<RestrictedIp> findByIp(String ip) {
        Document document = collection.find(
                Filters.eq("ip", ip)
        ).first();

        return Optional.ofNullable(document).map(doc ->
                new RestrictedIp(
                        doc.getString("ip"),
                        doc.getDate("blocked_until").toInstant()
                )
        );
    }

    @Override
    public boolean unblock(String ip) {
        DeleteResult del = collection.deleteOne(
                Filters.eq("ip")
        );

        return del.getDeletedCount() > 0;
    }
}
