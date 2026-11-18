package pl.nbd.benchmarks;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;

final class BenchmarkDatabase {
    static MongoClient open() {
        return MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(System.getProperty("benchmark.mongoUri",
                "mongodb://localhost:27027/?serverSelectionTimeoutMS=3000")))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true)
                    .conventions(List.of(Conventions.ANNOTATION_CONVENTION)).build())))
            .build());
    }
}
