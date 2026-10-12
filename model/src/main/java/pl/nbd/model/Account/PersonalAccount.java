package pl.nbd.model.Account;

import pl.nbd.model.Account.enums.AccountStatus;
import pl.nbd.model.Account.enums.AccountTier;
import pl.nbd.model.shared.Money;

import java.util.Objects;
import java.util.UUID;

public class PersonalAccount extends Account {
    private AccountTier tier;

    public PersonalAccount(UUID ownerId, AccountNumber accountNumber, Money balance,
                           AccountStatus status, AccountTier tier) {
        super(ownerId, accountNumber, balance, status);
        this.tier = Objects.requireNonNull(tier, "Tier must not be null");
        validateInitialState();
    }

    public AccountTier getTier() {
        return tier;
    }

    public Money getOverdraftLimit() {
        return limitFor(tier);
    }

    private static Money limitFor(AccountTier tier) {
        return switch (tier) {
            case DEFAULT -> Money.pln("0.00");
            case SILVER -> Money.pln("500.00");
            case GOLD -> Money.pln("2000.00");
            case DIAMOND -> Money.pln("5000.00");
        };
    }

    @Override
    protected Money minimumAllowedBalance() {
        return getOverdraftLimit().negate();
    }

    public void changeAccountTier(AccountTier newTier) {
        Objects.requireNonNull(newTier, "Tier must not be null");
        if (getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot change tier of a closed account");
        }
        if (getBalance().isLessThan(limitFor(newTier).negate())) {
            throw new IllegalArgumentException("Current balance exceeds the new overdraft limit");
        }
        tier = newTier;
    }
}
