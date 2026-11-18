package pl.nbd.infrastructure.redis;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.adapters.RedisOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.RedisOperationRepository;
import pl.nbd.infrastructure.util.RedisClientFactory;
import redis.clients.jedis.JedisPooled;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "redis.integration", matches = "true")
class RedisOperationIntegrationTest {
    @Test
    void writesReadsAndInvalidatesRealRedis() throws Exception {
        OperationMapper mapper = new OperationMapper();
        try (JedisPooled redis = RedisClientFactory.create(); Jsonb jsonb = JsonbBuilder.create()) {
            RedisOperationRepository repository = new RedisOperationRepository(redis, RedisClientFactory.ttlSeconds(), jsonb);
            RedisOperationTest.FakeMongo mongo = new RedisOperationTest.FakeMongo();
            RedisOperationRepositoryAdapter adapter = new RedisOperationRepositoryAdapter(mongo, repository, mapper);
            AtmDeposit operation = new AtmDeposit(Money.pln("12.34"), UUID.randomUUID(), "TEST");
            String key = operation.getId().toString();
            try {
                adapter.save(operation);
                assertTrue(repository.findById(operation.getId()).isEmpty());
                assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
                var restored = adapter.findById(operation.getId()).orElseThrow();
                assertEquals(operation.getId(), restored.getId());
                assertEquals(operation.getAmount(), restored.getAmount());
                assertEquals(operation.getOccurredAt(), restored.getOccurredAt());
                assertEquals(1, mongo.reads);
                assertNotNull(redis.get(key));
                long ttl = redis.ttl(key);
                assertTrue(ttl > 0 && ttl <= RedisClientFactory.ttlSeconds());
                adapter.invalidate(operation.getId());
                assertNull(redis.get(key));
                assertTrue(repository.findById(operation.getId()).isEmpty());
                assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
                assertEquals(2, mongo.reads);
            } finally {
                repository.deleteById(operation.getId());
            }
        }
    }

    @Test
    void redisActuallyExpiresTheEntry() throws Exception {
        OperationMapper mapper = new OperationMapper();
        try (JedisPooled redis = RedisClientFactory.create(); Jsonb jsonb = JsonbBuilder.create()) {
            RedisOperationRepository repository = new RedisOperationRepository(redis, 1, jsonb);
            AtmDeposit operation = new AtmDeposit(Money.pln("12.34"), UUID.randomUUID(), "TEST");
            try {
                repository.save(mapper.toDocument(operation));
                assertTrue(repository.findById(operation.getId()).isPresent());
                long deadline = System.nanoTime() + 5_000_000_000L;
                while (repository.findById(operation.getId()).isPresent() && System.nanoTime() < deadline) {
                    Thread.sleep(25);
                }
                assertTrue(repository.findById(operation.getId()).isEmpty());
            } finally {
                repository.deleteById(operation.getId());
            }
        }
    }

    @Test
    void realConnectionFailureFallsBackToDelegate() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        try (JedisPooled redis = new JedisPooled(URI.create("redis://127.0.0.1:" + port), 100);
             Jsonb jsonb = JsonbBuilder.create()) {
            RedisOperationTest.FakeMongo mongo = new RedisOperationTest.FakeMongo();
            RedisOperationRepository repository = new RedisOperationRepository(redis, 300, jsonb);
            RedisOperationRepositoryAdapter adapter = new RedisOperationRepositoryAdapter(
                mongo, repository, new OperationMapper());
            AtmDeposit operation = new AtmDeposit(Money.pln("12.34"), UUID.randomUUID(), "TEST");
            assertDoesNotThrow(() -> adapter.save(operation));
            assertSame(operation, mongo.values.get(operation.getId()));
            assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
            assertEquals(1, mongo.reads);
            assertDoesNotThrow(() -> adapter.invalidate(operation.getId()));
        }
    }
}
