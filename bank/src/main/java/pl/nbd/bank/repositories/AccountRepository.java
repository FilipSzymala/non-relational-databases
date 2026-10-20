package pl.nbd.bank.repositories;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
    Optional<Account> findByAccountNumber(AccountNumber accountNumber);
    List<Account> findByOwnerId(UUID ownerId);
    void save(Account account);
    void deleteById(UUID id);
}
