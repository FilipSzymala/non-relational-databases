package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.shared.Money;

import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.account.AccountNumberEmbeddable;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;

public class AccountMapper {

    public AccountEntity toEntity(Account account, ClientEntity client) {

        AccountNumberEmbeddable number = new AccountNumberEmbeddable(account.getAccountNumber().number());

        var status = pl.nbd.infrastructure.entities.account.enums.AccountStatus.valueOf(account.getStatus().name());

        if (account instanceof PersonalAccount personal) {

            var tier = pl.nbd.infrastructure.entities.account.enums.AccountTier.valueOf(personal.getTier().name());

            return PersonalAccountEntity.create(personal.getId(), client, personal.getCreatedAt(), number, personal.getBalance().amount(), status, tier);
        }

        if (account instanceof SavingsAccount savings) {

            return SavingsAccountEntity.create(
                savings.getId(), client, savings.getCreatedAt(),
                number, savings.getBalance().amount(), status, savings.getAnnualInterestRate(), savings.getLastInterestCalculationDate()
            );
        }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass());
    }

    public Account toDomain(AccountEntity entity) {

        AccountNumber number = new AccountNumber(entity.getAccountNumberEmbeddable().getAccountNumber());

        Money balance = new Money(entity.getBalance());

        var status = pl.nbd.bank.bank.account.enums.AccountStatus.valueOf(entity.getStatus().name());

        if (entity instanceof PersonalAccountEntity personal) {

            var tier = pl.nbd.bank.bank.account.enums.AccountTier.valueOf(personal.getTier().name());

            return new PersonalAccount(personal.getId(), personal.getClient().getId(), personal.getCreatedAt(), number, balance, status, tier);
        }

        if (entity instanceof SavingsAccountEntity savings) {

            return new SavingsAccount(
                savings.getId(), savings.getClient().getId(), savings.getCreatedAt(),
                number, balance, status, savings.getAnnualInterestRate(), savings.getLastInterestCalculationDate()
            );
        }

        throw new IllegalArgumentException("Unsupported account entity type: " + entity.getClass());
    }
}