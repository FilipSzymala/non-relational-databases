package pl.nbd.infrastructure.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;

public abstract class AbstractMongoRepository implements AutoCloseable {
    private ConnectionString connectionString = new ConnectionString(
        "mongodb://mongodb1:27017,mongodb2:27018,mongodb3:27019/?replicaSet=replica_set_single"
    );

    private MongoCredential mongoCredential = MongoCredential.createCredential(
        "admin", "admin", "adminpassword".toCharArray()
    );

    private CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(
        PojoCodecProvider.builder()
            .automatic(true)
            .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
            .build()
    );

    private void initDbConnection() {
        MongoClientSettings settings = MongoClientSettings.builder()
            .credential(mongoCredential)
            .applyConnectionString(connectionString)
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(pojoCodecRegistry)
            .build();

        mongoClient = MongoClients.create(settings);
        bankDB = mongoClient.getDatabase("bank");
    }

    private MongoClient mongoClient;

    private MongoDatabase bankDB;
}
