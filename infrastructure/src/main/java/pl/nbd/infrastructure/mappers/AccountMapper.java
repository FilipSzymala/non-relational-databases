package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.documents.account.AbstractAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.PersonalAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.SavingsAccountEmbeddedMgd;

import java.time.YearMonth;
import java.util.Objects;

public class AccountMapper {

    public AbstractAccountEmbeddedMgd toDocument(Account account) {
        Objects.requireNonNull(account, "Account must not be null");

        if (account instanceof PersonalAccount personalAccount) {
            return new PersonalAccountEmbeddedMgd(
                personalAccount.getId(),
                personalAccount.getOwnerId(),
                personalAccount.getCreatedAt(),
                personalAccount.getAccountNumber().number(),
                personalAccount.getBalance().amount(),
                toDocumentStatus(personalAccount.getStatus()),
                toDocumentTier(personalAccount.getTier())
            );
        }

        if (account instanceof SavingsAccount savingsAccount) {
            return new SavingsAccountEmbeddedMgd(
                savingsAccount.getId(),
                savingsAccount.getOwnerId(),
                savingsAccount.getCreatedAt(),
                savingsAccount.getAccountNumber().number(),
                savingsAccount.getBalance().amount(),
                toDocumentStatus(savingsAccount.getStatus()),
                savingsAccount.getAnnualInterestRate(),
                toDocumentMonth(savingsAccount.getLastInterestCalculationDate())
            );
        }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getName());
    }

    public Account toDomain(AbstractAccountEmbeddedMgd document) {
        Objects.requireNonNull(document, "Account document must not be null");

        if (document instanceof PersonalAccountEmbeddedMgd personalAccount) {
            return new PersonalAccount(
                personalAccount.getId(),
                personalAccount.getOwnerId(),
                personalAccount.getCreatedAt(),
                new AccountNumber(personalAccount.getAccountNumber()),
                new Money(personalAccount.getBalance()),
                toDomainStatus(personalAccount.getStatus()),
                toDomainTier(personalAccount.getAccountTier())
            );
        }

        if (document instanceof SavingsAccountEmbeddedMgd savingsAccount) {
            return new SavingsAccount(
                savingsAccount.getId(),
                savingsAccount.getOwnerId(),
                savingsAccount.getCreatedAt(),
                new AccountNumber(savingsAccount.getAccountNumber()),
                new Money(savingsAccount.getBalance()),
                toDomainStatus(savingsAccount.getStatus()),
                savingsAccount.getAnnualInterestRate(),
                toDomainMonth(savingsAccount.getLastInterestCalculationDate())
            );
        }

        throw new IllegalArgumentException("Unsupported account document type: " + document.getClass().getName());
    }

    private static String toDocumentMonth(YearMonth month) {
        return month == null ? null : month.toString();
    }

    private static YearMonth toDomainMonth(String month) {
        return month == null ? null : YearMonth.parse(month);
    }

    private static pl.nbd.infrastructure.documents.account.enums.AccountStatus toDocumentStatus(AccountStatus status) {
        return pl.nbd.infrastructure.documents.account.enums.AccountStatus.valueOf(status.name());
    }

    private static AccountStatus toDomainStatus(pl.nbd.infrastructure.documents.account.enums.AccountStatus status) {
        return AccountStatus.valueOf(status.name());
    }

    private static pl.nbd.infrastructure.documents.account.enums.AccountTier toDocumentTier(AccountTier tier) {
        return pl.nbd.infrastructure.documents.account.enums.AccountTier.valueOf(tier.name());
    }

    private static AccountTier toDomainTier(pl.nbd.infrastructure.documents.account.enums.AccountTier tier) {
        return AccountTier.valueOf(tier.name());
    }
}
