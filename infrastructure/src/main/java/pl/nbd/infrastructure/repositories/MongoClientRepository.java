package pl.nbd.infrastructure.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import pl.nbd.infrastructure.documents.client.ClientEmbeddedMgd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class MongoClientRepository extends AbstractMongoRepository {

    private final MongoCollection<ClientEmbeddedMgd> collection;

    public MongoClientRepository(MongoClient mongoClient) {
        super(mongoClient);
        this.collection = getDatabase().getCollection("clients", ClientEmbeddedMgd.class);
    }

    public void save(ClientEmbeddedMgd client) {
        Objects.requireNonNull(client, "Client must not be null");

        collection.replaceOne(eq("_id", client.getId()), client, new ReplaceOptions().upsert(true));
    }

    public Optional<ClientEmbeddedMgd> findById(UUID id) {
        Objects.requireNonNull(id, "Client id must not be null");

        return Optional.ofNullable(collection.find(eq("_id", id)).first());
    }

    public Optional<ClientEmbeddedMgd> findByPersonalId(String personalId) {
        Objects.requireNonNull(personalId, "Personal ID must not be null");

        return Optional.ofNullable(collection.find(eq("personal_id.value", personalId)).first());
    }

    public List<ClientEmbeddedMgd> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "Client id must not be null");

        return collection.deleteOne(eq("_id", id)).getDeletedCount() > 0;
    }
}