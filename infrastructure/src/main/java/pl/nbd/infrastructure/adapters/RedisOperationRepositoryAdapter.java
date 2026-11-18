package pl.nbd.infrastructure.adapters;

import jakarta.json.bind.JsonbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.RedisOperationRepository;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RedisOperationRepositoryAdapter implements OperationRepository {
    private static final Logger logger = LoggerFactory.getLogger(RedisOperationRepositoryAdapter.class);
    private final OperationRepository delegate;
    private final RedisOperationRepository repository;
    private final OperationMapper mapper;

    public RedisOperationRepositoryAdapter(OperationRepository delegate, RedisOperationRepository repository,
                                           OperationMapper mapper) {
        this.delegate = Objects.requireNonNull(delegate);
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<Operation> findById(UUID id) {
        Objects.requireNonNull(id);
        try {
            Optional<Operation> cached = findCached(id);
            if (cached.isPresent()) {
                return cached;
            }
        } catch (JedisException exception) {
            logger.debug("Redis read failed; using MongoDB", exception);
            return delegate.findById(id);
        }
        Optional<Operation> result = delegate.findById(id);
        result.ifPresent(operation -> {
            try {
                repository.save(mapper.toDocument(operation));
            } catch (JedisException | JsonbException exception) {
                logger.debug("Redis fill failed", exception);
            }
        });
        return result;
    }

    private Optional<Operation> findCached(UUID id) {
        return repository.findById(id).flatMap(document -> {
            try {
                return Optional.of(mapper.toDomain(document));
            } catch (RuntimeException exception) {
                logger.debug("Invalid cached operation; using MongoDB", exception);
                invalidate(id);
                return Optional.empty();
            }
        });
    }

    @Override
    public List<Operation> findByAccountId(UUID accountId) {
        return delegate.findByAccountId(accountId);
    }

    @Override
    public void save(Operation operation) {
        delegate.save(operation);
        invalidate(operation.getId());
    }

    public void invalidate(UUID id) {
        try {
            repository.deleteById(id);
        } catch (JedisException exception) {
            logger.debug("Redis invalidation failed", exception);
        }
    }
}
