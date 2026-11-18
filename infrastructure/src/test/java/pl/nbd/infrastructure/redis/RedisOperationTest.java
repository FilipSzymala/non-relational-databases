package pl.nbd.infrastructure.redis;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.spi.JsonbProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.operation.*;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.adapters.RedisOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.RedisOperationRepository;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RedisOperationTest {
    private final FakeRedis redis = new FakeRedis();
    private final Jsonb jsonb = JsonbBuilder.create();
    private final RedisOperationRepository repository = new RedisOperationRepository(redis, 300, jsonb);
    private final OperationMapper mapper = new OperationMapper();
    private final FakeMongo mongo = new FakeMongo();
    private final RedisOperationRepositoryAdapter adapter = new RedisOperationRepositoryAdapter(mongo, repository, mapper);
    private final Operation operation = new AtmDeposit(Money.pln("123.45"), UUID.randomUUID(), "ATM-1");

    @AfterEach
    void closeResources() throws Exception {
        try {
            jsonb.close();
        } finally {
            redis.close();
        }
    }

    @Test
    void usesYassonAsJsonbProvider() {
        assertEquals("org.eclipse.yasson.JsonBindingProvider", JsonbProvider.provider().getClass().getName());
    }

    @Test
    void writesJsonAndReadsAllOperationTypesWithoutLosingPrecision() {
        UUID account = UUID.randomUUID();
        Instant time = Instant.parse("2025-01-01T12:00:00.123456789Z");
        Money amount = Money.pln("123456789012345.67");
        List<Operation> values = List.of(
            new AtmDeposit(UUID.randomUUID(), time, amount, account, "Bankomat \"Łódź\""),
            new AtmWithdraw(UUID.randomUUID(), time, amount, account, "ATM-2"),
            new InterestCapitalization(UUID.randomUUID(), time, amount, account, YearMonth.of(2025, 1)),
            new Transfer(UUID.randomUUID(), time, amount, account, UUID.randomUUID()));
        for (Operation value : values) {
            repository.save(mapper.toDocument(value));
            assertTrue(redis.values.containsKey(value.getId().toString()));
            Operation restored = repository.findById(value.getId()).map(mapper::toDomain).orElseThrow();
            assertOperationEquals(value, restored);
        }
        CommandArguments arguments = new CommandArguments(Protocol.Command.SET);
        redis.parameters.addParams(arguments);
        List<String> command = new ArrayList<>();
        arguments.forEach(argument -> command.add(new String(argument.getRaw(), StandardCharsets.UTF_8)));
        assertEquals(List.of("SET", "EX", "300"), command);
    }

    @Test
    void missingKeyReturnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void invalidatesOnlyRequestedOperation() {
        repository.save(mapper.toDocument(operation));
        redis.values.put("unrelated", "value");
        repository.deleteById(operation.getId());
        assertTrue(repository.findById(operation.getId()).isEmpty());
        assertEquals("value", redis.values.get("unrelated"));
    }

    @Test
    void missLoadsMongoAndNextReadHitsCache() {
        mongo.save(operation);
        assertEquals(operation.getId(), adapter.findById(operation.getId()).orElseThrow().getId());
        assertEquals(operation.getId(), adapter.findById(operation.getId()).orElseThrow().getId());
        assertEquals(1, mongo.reads);
    }

    @Test
    void doesNotCacheAbsenceSoLaterInsertIsVisible() {
        assertTrue(adapter.findById(operation.getId()).isEmpty());
        mongo.save(operation);
        assertTrue(adapter.findById(operation.getId()).isPresent());
        assertEquals(2, mongo.reads);
    }

    @Test
    void saveInvalidatesAfterMongoSucceeds() {
        repository.save(mapper.toDocument(operation));
        adapter.save(operation);
        assertTrue(repository.findById(operation.getId()).isEmpty());
        assertSame(operation, mongo.values.get(operation.getId()));
    }

    @Test
    void failedMongoWriteIsPropagatedAndDoesNotInvalidate() {
        repository.save(mapper.toDocument(operation));
        mongo.failWrite = true;
        assertThrows(IllegalStateException.class, () -> adapter.save(operation));
        assertTrue(repository.findById(operation.getId()).isPresent());
    }

    @Test
    void readWriteAndInvalidationSurviveRedisOutageAndRecovery() {
        redis.offline = true;
        adapter.save(operation);
        adapter.invalidate(operation.getId());
        assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
        redis.offline = false;
        assertTrue(adapter.findById(operation.getId()).isPresent());
        assertTrue(repository.findById(operation.getId()).isPresent());
    }

    @Test
    void failedCacheFillDoesNotLoseMongoResult() {
        mongo.save(operation);
        redis.failSet = true;
        assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
    }

    @Test
    void accountHistoryAlwaysUsesMongo() {
        mongo.save(operation);
        assertEquals(List.of(operation), adapter.findByAccountId(operation.affectedAccounts().iterator().next()));
        assertTrue(redis.values.isEmpty());
    }

    @Test
    void corruptJsonIsRemovedAndReloaded() {
        mongo.save(operation);
        redis.values.put(operation.getId().toString(), "invalid json");
        assertSame(operation, adapter.findById(operation.getId()).orElseThrow());
        assertTrue(repository.findById(operation.getId()).isPresent());
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThrows(IllegalArgumentException.class, () -> new RedisOperationRepository(redis, 0, jsonb));
        assertThrows(IllegalArgumentException.class, () -> new RedisOperationRepository(redis, -1, jsonb));
    }

    @Test
    void invalidCachedDataIsRemovedAndReloadedFromMongo() {
        mongo.save(operation);
        String validJson = """
            {"_t":"atm_deposit","id":"%s","occurredAt":"2025-01-01T12:00:00Z","amount":123.45,
             "accountId":"%s","atmId":"ATM-1"}
            """.formatted(operation.getId(), operation.affectedAccounts().iterator().next());
        List<String> invalidValues = List.of(
            "null",
            validJson.replace(operation.getId().toString(), UUID.randomUUID().toString()),
            validJson.replace("atm_deposit", "unsupported"),
            validJson.replace("\"amount\":123.45,", ""),
            validJson.replace("\"accountId\":\"" + operation.affectedAccounts().iterator().next() + "\",", ""),
            validJson.replace("123.45", "-123.45"),
            validJson.replace("123.45", "123.456"),
            validJson.replace("2025-01-01T12:00:00Z", "invalid timestamp"));
        String key = operation.getId().toString();

        for (String invalidJson : invalidValues) {
            redis.values.put(key, invalidJson);
            redis.deletedKeys.clear();
            assertSame(operation, adapter.findById(operation.getId()).orElseThrow(), invalidJson);
            assertEquals(List.of(key), redis.deletedKeys, invalidJson);
            Operation restored = repository.findById(operation.getId()).map(mapper::toDomain).orElseThrow();
            assertOperationEquals(operation, restored);
        }
        assertEquals(invalidValues.size(), mongo.reads);
    }

    private static void assertOperationEquals(Operation expected, Operation actual) {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getOccurredAt(), actual.getOccurredAt());
        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.affectedAccounts(), actual.affectedAccounts());
        switch (expected) {
            case AtmDeposit deposit -> {
                AtmDeposit restored = assertInstanceOf(AtmDeposit.class, actual);
                assertEquals(deposit.getAccountId(), restored.getAccountId());
                assertEquals(deposit.getAtmId(), restored.getAtmId());
            }
            case AtmWithdraw withdraw -> {
                AtmWithdraw restored = assertInstanceOf(AtmWithdraw.class, actual);
                assertEquals(withdraw.getAccountId(), restored.getAccountId());
                assertEquals(withdraw.getAtmId(), restored.getAtmId());
            }
            case InterestCapitalization capitalization -> {
                InterestCapitalization restored = assertInstanceOf(InterestCapitalization.class, actual);
                assertEquals(capitalization.getAccountId(), restored.getAccountId());
                assertEquals(capitalization.getMonth(), restored.getMonth());
            }
            case Transfer transfer -> {
                Transfer restored = assertInstanceOf(Transfer.class, actual);
                assertEquals(transfer.getSourceAccountId(), restored.getSourceAccountId());
                assertEquals(transfer.getTargetAccountId(), restored.getTargetAccountId());
            }
            default -> fail("Unsupported operation type: " + expected.getClass().getName());
        }
    }

    static class FakeRedis extends JedisPooled {
        final Map<String, String> values = new HashMap<>();
        final List<String> deletedKeys = new ArrayList<>();
        boolean offline;
        boolean failSet;
        SetParams parameters;

        FakeRedis() {
            super("localhost", 1);
        }

        private void check() {
            if (offline) {
                throw new JedisConnectionException("Simulated outage");
            }
        }

        @Override
        public String set(String key, String value, SetParams params) {
            check();
            if (failSet) {
                throw new JedisConnectionException("Simulated write outage");
            }
            parameters = params;
            values.put(key, value);
            return "OK";
        }

        @Override
        public String get(String key) {
            check();
            return values.get(key);
        }

        @Override
        public long del(String key) {
            check();
            deletedKeys.add(key);
            return values.remove(key) == null ? 0 : 1;
        }
    }

    static class FakeMongo implements OperationRepository {
        final Map<UUID, Operation> values = new HashMap<>();
        int reads;
        boolean failWrite;

        public Optional<Operation> findById(UUID id) {
            reads++;
            return Optional.ofNullable(values.get(id));
        }

        public List<Operation> findByAccountId(UUID id) {
            return values.values().stream().filter(value -> value.affectedAccounts().contains(id)).toList();
        }

        public void save(Operation operation) {
            if (failWrite) {
                throw new IllegalStateException("Simulated Mongo failure");
            }
            values.put(operation.getId(), operation);
        }
    }
}
