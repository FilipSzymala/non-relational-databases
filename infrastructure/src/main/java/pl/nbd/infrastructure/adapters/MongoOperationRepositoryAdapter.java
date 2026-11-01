package pl.nbd.infrastructure.adapters;

import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MongoOperationRepositoryAdapter implements OperationRepository {

    private final MongoOperationRepository repository;
    private final OperationMapper mapper;

    public MongoOperationRepositoryAdapter(MongoOperationRepository repository, OperationMapper mapper) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<Operation> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Operation> findByAccountId(UUID accountId) {
        return repository.findByAccountId(accountId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void save(Operation operation) {
        repository.insert(mapper.toDocument(operation));
    }
}