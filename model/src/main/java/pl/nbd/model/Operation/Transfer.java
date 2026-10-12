package pl.nbd.model.Operation;

import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A completed transfer between two different accounts.
 */
public final class Transfer extends Operation {
    private final UUID sourceAccountId;
    private final UUID targetAccountId;

    public Transfer(Money amount, UUID sourceAccountId, UUID targetAccountId) {
        super(amount);
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source account id must not be null");
        this.targetAccountId = Objects.requireNonNull(targetAccountId, "Target account id must not be null");
        validateDistinctAccounts();
    }

    public Transfer(UUID id, Instant occurredAt, Money amount, UUID sourceAccountId, UUID targetAccountId) {
        super(id, occurredAt, amount);
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source account id must not be null");
        this.targetAccountId = Objects.requireNonNull(targetAccountId, "Target account id must not be null");
        validateDistinctAccounts();
    }

    private void validateDistinctAccounts() {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    @Override
    public Set<UUID> affectedAccounts() {
        return Set.of(sourceAccountId, targetAccountId);
    }
}
