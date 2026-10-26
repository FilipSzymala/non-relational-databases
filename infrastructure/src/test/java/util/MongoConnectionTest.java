package util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.nbd.infrastructure.util.AbstractMongoRepository;
import pl.nbd.infrastructure.util.MongoClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoConnectionTest {
    @Test
    void shouldConnectToReplicationSet() {
        try (MongoClient client = MongoClientFactory.create("admin", "adminpassword")) {
            MongoDatabase db = client.getDatabase("bank");

            Document doc = db.runCommand(new Document("hello", "world"));

            assertEquals(
                "replica_set_single",
                doc.getString("setName")
            );

            assertTrue(
                doc.getBoolean("isWritablePrimary", false)
            );

            assertEquals(
                3,
                doc.getList("hosts", String.class).size()
            );
        }
    }
}
