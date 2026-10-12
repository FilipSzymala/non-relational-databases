package pl.nbd.model.Account;

import pl.nbd.model.Account.enums.AccountStatus;
import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public abstract class Account {
    private final UUID id;
    private final UUID ownerId;
    private final Instant createdAt;
    private final AccountNumber accountNumber;
    private Money balance;
    private AccountStatus status;

    public Account(UUID ownerId, AccountNumber accountNumber, Money balance, AccountStatus status) {
        this.id = UUID.randomUUID();
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID must not be null");
        this.createdAt = Instant.now();
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number must not be null");
        this.balance = Objects.requireNonNull(balance, "Balance must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AccountNumber getAccountNumber() {
        return accountNumber;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    protected void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }

    protected final void validateInitialState() {
        if (balance.isLessThan(minimumAllowedBalance())) {
            throw new IllegalArgumentException("Initial balance exceeds allowed overdraft");
        }
        if (status == AccountStatus.CLOSED && !balance.isZero()) {
            throw new IllegalArgumentException("Closed account must have zero balance");
        }
    }

    protected abstract Money minimumAllowedBalance();

    public void credit(Money amount) {
        requireActive();
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void debit(Money amount) {
        requireActive();
        requirePositive(amount);
        Money newBalance = balance.subtract(amount);
        if (newBalance.isLessThan(minimumAllowedBalance())) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balance = newBalance;
    }

    private static void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public void block() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active accounts can be blocked");
        }
        status = AccountStatus.BLOCKED;
    }

    public void unblock() {
        if (status != AccountStatus.BLOCKED) {
            throw new IllegalStateException("Only blocked accounts can be unblocked");
        }
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (!balance.isZero()) {
            throw new IllegalStateException("Account must have zero balance to be closed");
        }
        status = AccountStatus.CLOSED;
    }
}
