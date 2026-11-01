package pl.nbd.infrastructure.adapters;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.repository.AccountRepository;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MongoAccountRepositoryAdapter implements AccountRepository {

    private final MongoAccountRepository repository;
    private final AccountMapper mapper;

    public MongoAccountRepositoryAdapter(MongoAccountRepository repository, AccountMapper mapper) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        Objects.requireNonNull(accountNumber, "Account number must not be null");

        return repository.findByAccountNumber(accountNumber.number()).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByOwnerId(UUID ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        repository.save(mapper.toDocument(account));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}