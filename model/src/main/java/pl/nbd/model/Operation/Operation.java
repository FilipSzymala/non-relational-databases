package pl.nbd.model.Operation;

import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class Operation {
    private final UUID id;
    private final Instant occurredAt;
    private final Money amount;

    protected Operation(Money amount) {
        this(UUID.randomUUID(), Instant.now(), amount);
    }

    protected Operation(UUID id, Instant occurredAt, Money amount) {
        this.id = Objects.requireNonNull(id, "Operation id must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Operation timestamp must not be null");
        this.amount = Objects.requireNonNull(amount, "Operation amount must not be null");
        Objects.requireNonNull(amount.amount(), "Money amount must not be null");
        Objects.requireNonNull(amount.currency(), "Money currency must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("Operation amount must be positive");
        }
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Money getAmount() {
        return amount;
    }

    /**
     * Account identifiers affected by this operation.
     */
    public abstract Set<UUID> affectedAccounts();
}
