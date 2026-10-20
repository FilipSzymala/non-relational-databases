package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.repositories.AccountRepository;
import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.repositories.JpaAccountRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaAccountRepositoryAdapter implements AccountRepository {

    private final EntityManager em;
    private final JpaAccountRepository repository;
    private final AccountMapper mapper;

    public JpaAccountRepositoryAdapter(EntityManager em) {
        this.em = Objects.requireNonNull(em);
        this.repository = new JpaAccountRepository(em);
        this.mapper = new AccountMapper();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        Objects.requireNonNull(accountNumber);
        return repository.findByAccountNumber(accountNumber.number()).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByOwnerId(UUID ownerId) {
        return repository.findByOwnerId(ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account);

        ClientEntity client = em.find(ClientEntity.class, account.getOwnerId());
        if (client == null) {
            throw new IllegalArgumentException("Client does not exist: " + account.getOwnerId());
        }

        AccountEntity mapped = mapper.toEntity(account, client);
        Optional<AccountEntity> existing = repository.findById(account.getId());

        if (existing.isEmpty()) {
            repository.persist(mapped);
            return;
        }

        AccountEntity current = existing.get();
        if (!current.getClass().equals(mapped.getClass())) {
            throw new IllegalArgumentException("Account type cannot be changed: " + account.getId());
        }
        if (!current.getClient().getId().equals(account.getOwnerId())) {
            throw new IllegalArgumentException("Account owner cannot be changed: " + account.getId());
        }

        em.merge(mapped);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
