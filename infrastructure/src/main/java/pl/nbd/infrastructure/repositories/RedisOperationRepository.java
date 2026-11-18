package pl.nbd.infrastructure.repositories;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import pl.nbd.infrastructure.documents.operation.AbstractOperationEmbeddedMgd;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RedisOperationRepository {
    private final UnifiedJedis redis;
    private final long ttlSeconds;
    private final Jsonb jsonb;

    public RedisOperationRepository(UnifiedJedis redis, long ttlSeconds, Jsonb jsonb) {
        this.redis = Objects.requireNonNull(redis);
        this.jsonb = Objects.requireNonNull(jsonb);
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        this.ttlSeconds = ttlSeconds;
    }

    public void save(AbstractOperationEmbeddedMgd operation) {
        Objects.requireNonNull(operation, "Operation document must not be null");
        redis.set(operation.getId().toString(), jsonb.toJson(operation), SetParams.setParams().ex(ttlSeconds));
    }

    public Optional<AbstractOperationEmbeddedMgd> findById(UUID id) {
        String value = redis.get(id.toString());
        if (value == null) {
            return Optional.empty();
        }

        AbstractOperationEmbeddedMgd operation;
        try {
            operation = jsonb.fromJson(value, AbstractOperationEmbeddedMgd.class);
        } catch (JsonbException exception) {
            deleteById(id);
            return Optional.empty();
        }
        if (operation == null || !id.equals(operation.getId())) {
            deleteById(id);
            return Optional.empty();
        }
        return Optional.of(operation);
    }

    public void deleteById(UUID id) {
        redis.del(id.toString());
    }
}
