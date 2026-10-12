package pl.nbd.model.Operation;

import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Interest added to a savings account for a given month.
 */
public final class InterestCapitalization extends Operation {
    private final UUID accountId;
    private final YearMonth month;

    public InterestCapitalization(Money amount, UUID accountId, YearMonth month) {
        super(amount);
        this.accountId = Objects.requireNonNull(accountId, "Account id must not be null");
        this.month = Objects.requireNonNull(month, "Month must not be null");
    }

    public InterestCapitalization(UUID id, Instant occurredAt, Money amount, UUID accountId, YearMonth month) {
        super(id, occurredAt, amount);
        this.accountId = Objects.requireNonNull(accountId, "Account id must not be null");
        this.month = Objects.requireNonNull(month, "Month must not be null");
    }

    public UUID getAccountId() {
        return accountId;
    }

    public YearMonth getMonth() {
        return month;
    }

    @Override
    public Set<UUID> affectedAccounts() {
        return Set.of(accountId);
    }
}
