package pl.nbd.model.repository;

import pl.nbd.model.Account.Account;
import pl.nbd.model.Account.AccountNumber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
    Optional<Account> findByAccountNumber(AccountNumber accountNumber);
    List<Account> findByOwnerId(UUID ownerId);
    void save(Account account);
}
