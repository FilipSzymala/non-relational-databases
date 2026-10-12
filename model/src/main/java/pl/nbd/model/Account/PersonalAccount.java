package pl.nbd.model.Account;

import pl.nbd.model.Account.enums.AccountStatus;
import pl.nbd.model.Account.enums.AccountTier;
import pl.nbd.model.Money;

import java.util.UUID;

public class PersonalAccount extends Account {
    private AccountTier tier;
    private Money overdraftLimit;

    public PersonalAccount(UUID ownerId, AccountNumber accountNumber, Money balance, AccountStatus status, AccountTier tier, Money overdraftLimit) {
        super(ownerId, accountNumber, balance, status);
        this.tier = tier;
        this.overdraftLimit = overdraftLimit;
    }

    public AccountTier getTier() {
        return tier;
    }

    public Money getOverdraftLimit() {
        return overdraftLimit;
    }

    public void changeAccountTier(AccountTier newTier) {
        this.tier = newTier;
    }
}

