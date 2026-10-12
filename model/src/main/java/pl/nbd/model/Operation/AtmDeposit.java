package pl.nbd.model.Operation;

import pl.nbd.model.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A cash deposit into one bank account.
 */
public final class AtmDeposit extends Operation {
    private final UUID accountId;
    private final String atmId;

    public AtmDeposit(Money amount, UUID accountId, String atmId) {
        super(amount);
        this.accountId = Objects.requireNonNull(accountId, "Account id must not be null");
        this.atmId = requireAtmId(atmId);
    }

    public AtmDeposit(UUID id, Instant occurredAt, Money amount, UUID accountId, String atmId) {
        super(id, occurredAt, amount);
        this.accountId = Objects.requireNonNull(accountId, "Account id must not be null");
        this.atmId = requireAtmId(atmId);
    }

    private static String requireAtmId(String atmId) {
        if (atmId == null || atmId.isBlank()) {
            throw new IllegalArgumentException("ATM id must not be blank");
        }
        return atmId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAtmId() {
        return atmId;
    }

    @Override
    public Set<UUID> affectedAccounts() {
        return Set.of(accountId);
    }
}
