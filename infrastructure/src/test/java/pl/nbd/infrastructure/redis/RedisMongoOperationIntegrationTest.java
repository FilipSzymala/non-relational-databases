package pl.nbd.infrastructure.redis;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.adapters.MongoOperationRepositoryAdapter;
import pl.nbd.infrastructure.adapters.RedisOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;
import pl.nbd.infrastructure.repositories.RedisOperationRepository;
import pl.nbd.infrastructure.util.RedisClientFactory;
import redis.clients.jedis.JedisPooled;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "redis.mongo.integration", matches = "true")
class RedisMongoOperationIntegrationTest {
    @Test
    void decoratesExistingMongoAdapterAndUsesRedisForRepeatedReads() throws Exception {
        AtomicInteger mongoReads = new AtomicInteger();
        try (MongoClient mongoClient = openMongo(mongoReads);
             JedisPooled redis = RedisClientFactory.create();
             Jsonb jsonb = JsonbBuilder.create()) {
            MongoOperationRepositoryAdapter mongo = new MongoOperationRepositoryAdapter(
                new MongoOperationRepository(mongoClient), new OperationMapper());
            RedisOperationRepository cache = new RedisOperationRepository(redis, RedisClientFactory.ttlSeconds(), jsonb);
            OperationMapper mapper = new OperationMapper();
            RedisOperationRepositoryAdapter decorated = new RedisOperationRepositoryAdapter(mongo, cache, mapper);
            AtmDeposit operation = newOperation();
            try {
                cache.save(mapper.toDocument(operation));
                decorated.save(operation);
                assertTrue(cache.findById(operation.getId()).isEmpty());
                assertEquals(0, mongoReads.get());

                assertOperationEquals(operation, decorated.findById(operation.getId()).orElseThrow());
                assertEquals(1, mongoReads.get());
                assertTrue(cache.findById(operation.getId()).isPresent());

                assertOperationEquals(operation, decorated.findById(operation.getId()).orElseThrow());
                assertEquals(1, mongoReads.get());

                decorated.invalidate(operation.getId());
                assertTrue(cache.findById(operation.getId()).isEmpty());
                assertOperationEquals(operation, decorated.findById(operation.getId()).orElseThrow());
                assertEquals(2, mongoReads.get());
                assertTrue(cache.findById(operation.getId()).isPresent());
            } finally {
                try {
                    cache.deleteById(operation.getId());
                } finally {
                    deleteOperation(mongoClient, operation.getId());
                }
            }
        }
    }

    @Test
    void unavailableRedisStillSavesAndReadsUsingExistingMongoAdapter() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        AtomicInteger mongoReads = new AtomicInteger();
        try (MongoClient mongoClient = openMongo(mongoReads);
             JedisPooled redis = new JedisPooled(URI.create("redis://127.0.0.1:" + port), 100);
             Jsonb jsonb = JsonbBuilder.create()) {
            MongoOperationRepositoryAdapter mongo = new MongoOperationRepositoryAdapter(
                new MongoOperationRepository(mongoClient), new OperationMapper());
            RedisOperationRepository cache = new RedisOperationRepository(redis, 300, jsonb);
            RedisOperationRepositoryAdapter decorated = new RedisOperationRepositoryAdapter(
                mongo, cache, new OperationMapper());
            AtmDeposit operation = newOperation();
            try {
                assertDoesNotThrow(() -> decorated.save(operation));
                assertOperationEquals(operation, decorated.findById(operation.getId()).orElseThrow());
                assertEquals(1, mongoReads.get());
                assertOperationEquals(operation, mongo.findById(operation.getId()).orElseThrow());
                assertDoesNotThrow(() -> decorated.invalidate(operation.getId()));
            } finally {
                deleteOperation(mongoClient, operation.getId());
            }
        }
    }

    private static MongoClient openMongo(AtomicInteger reads) {
        return MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(System.getProperty("redis.mongo.uri",
                "mongodb://localhost:27027/?serverSelectionTimeoutMS=3000")))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true)
                    .conventions(List.of(Conventions.ANNOTATION_CONVENTION)).build())))
            .addCommandListener(new CommandListener() {
                @Override
                public void commandStarted(CommandStartedEvent event) {
                    if (event.getCommandName().equals("find") && event.getDatabaseName().equals("bank")
                        && event.getCommand().getString("find").getValue().equals("operations")) {
                        reads.incrementAndGet();
                    }
                }
            })
            .build());
    }

    private static AtmDeposit newOperation() {
        return new AtmDeposit(UUID.randomUUID(), Instant.parse("2026-11-17T12:00:00.123Z"),
            Money.pln("123.45"), UUID.randomUUID(), "INTEGRATION-ATM");
    }

    private static void assertOperationEquals(AtmDeposit expected, Operation actual) {
        AtmDeposit restored = assertInstanceOf(AtmDeposit.class, actual);
        assertEquals(expected.getId(), restored.getId());
        assertEquals(expected.getOccurredAt(), restored.getOccurredAt());
        assertEquals(expected.getAmount(), restored.getAmount());
        assertEquals(expected.getAccountId(), restored.getAccountId());
        assertEquals(expected.getAtmId(), restored.getAtmId());
    }

    private static void deleteOperation(MongoClient mongoClient, UUID id) {
        mongoClient.getDatabase("bank").getCollection("operations").deleteOne(eq("_id", id));
    }
}
