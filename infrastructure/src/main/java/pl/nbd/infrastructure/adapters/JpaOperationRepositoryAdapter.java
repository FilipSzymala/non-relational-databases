package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import pl.nbd.bank.bank.operation.InterestCapitalization;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.repositories.OperationRepository;
import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.JpaOperationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaOperationRepositoryAdapter implements OperationRepository {

    private final EntityManager em;
    private final JpaOperationRepository repository;
    private final OperationMapper mapper;

    public JpaOperationRepositoryAdapter(EntityManager em) {
        this.em = Objects.requireNonNull(em);
        this.repository = new JpaOperationRepository(em);
        this.mapper = new OperationMapper();
    }

    @Override
    public Optional<Operation> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Operation> findByAccountId(UUID accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void save(Operation operation) {
        Objects.requireNonNull(operation);
        if (repository.findById(operation.getId()).isPresent()) {
            throw new IllegalArgumentException("Operation already exists: " + operation.getId());
        }

        Map<UUID, AccountEntity> accounts = new HashMap<>();
        for (UUID accountId : operation.affectedAccounts()) {
            AccountEntity account;
            if (operation instanceof InterestCapitalization) {
                account = em.find(SavingsAccountEntity.class, accountId);
            } else {
                account = em.find(AccountEntity.class, accountId);
            }
            if (account == null) {
                throw new IllegalArgumentException("Account not found or invalid type: " + accountId);
            }
            accounts.put(accountId, account);
        }
        repository.persist(mapper.toEntity(operation, accounts));
    }
}
