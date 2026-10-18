package pl.nbd.bank.repository;

import pl.nbd.bank.bank.operation.Operation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationRepository {
    Optional<Operation> findById(UUID id);
    List<Operation> findByAccountId(UUID accountId);
    void save(Operation operation);
}
