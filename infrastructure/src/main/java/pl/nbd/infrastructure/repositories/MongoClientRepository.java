package pl.nbd.infrastructure.repositories;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.conversions.Bson;
import pl.nbd.infrastructure.documents.client.ClientEmbeddedMgd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class MongoClientRepository extends AbstractMongoRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<ClientEmbeddedMgd> collection;
    private final ClientSession session;

    public MongoClientRepository(MongoClient mongoClient) {
        this(mongoClient, null);
    }

    public MongoClientRepository(MongoClient mongoClient, ClientSession session) {
        super(mongoClient);
        this.mongoClient = mongoClient;
        this.collection = getDatabase().getCollection("clients", ClientEmbeddedMgd.class);
        this.session = session;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public boolean isSessionBound() {
        return session != null;
    }

    public void save(ClientEmbeddedMgd client) {
        Objects.requireNonNull(client, "Client must not be null");

        ReplaceOptions options = new ReplaceOptions().upsert(true);

        if (session == null) {
            collection.replaceOne(eq("_id", client.getId()), client, options);
        } else {
            collection.replaceOne(session, eq("_id", client.getId()), client, options);
        }
    }

    public Optional<ClientEmbeddedMgd> findById(UUID id) {
        Objects.requireNonNull(id, "Client id must not be null");

        return Optional.ofNullable(find(eq("_id", id)).first());
    }

    public Optional<ClientEmbeddedMgd> findByPersonalId(String personalId) {
        Objects.requireNonNull(personalId, "Personal ID must not be null");

        return Optional.ofNullable(find(eq("personal_id.value", personalId)).first());
    }

    public List<ClientEmbeddedMgd> findAll() {
        return find(new org.bson.Document()).into(new ArrayList<>());
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "Client id must not be null");

        if (session == null) {
            return collection.deleteOne(eq("_id", id)).getDeletedCount() > 0;
        }

        return collection.deleteOne(session, eq("_id", id)).getDeletedCount() > 0;
    }

    private FindIterable<ClientEmbeddedMgd> find(Bson filter) {
        return session == null ? collection.find(filter) : collection.find(session, filter);
    }
}