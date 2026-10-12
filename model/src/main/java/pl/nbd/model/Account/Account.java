package pl.nbd.model.Account;

import pl.nbd.model.Account.enums.AccountStatus;
import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.util.UUID;

public abstract class Account {
    private final UUID id;
    private UUID ownerId;
    private final Instant createdAt;
    private AccountNumber accountNumber;
    private Money balance;
    private AccountStatus status;

    public Account(UUID ownerId, AccountNumber accountNumber, Money balance, AccountStatus status) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AccountNumber getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(AccountNumber accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Money getBalance() {
        return balance;
    }

    public void setBalance(Money money) {
        this.balance = money;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void block() {
        this.status = AccountStatus.BLOCKED;
    }

    public void unblock() {
        this.status = AccountStatus.ACTIVE;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }
}
