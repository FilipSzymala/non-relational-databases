package pl.nbd.infrastructure.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import java.util.Objects;

public abstract class AbstractMongoRepository {
    private final MongoDatabase bankDB;

    protected AbstractMongoRepository(MongoClient client) {
        Objects.requireNonNull(client);
        this.bankDB = client.getDatabase("bank");
    }

    protected MongoDatabase getDatabase() {
        return bankDB;
    }
}