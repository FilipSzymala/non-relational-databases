package pl.nbd.infrastructure.mappers;

import org.junit.jupiter.api.Test;

import pl.nbd.bank.bank.account.*;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;

import pl.nbd.infrastructure.entities.account.*;
import pl.nbd.infrastructure.entities.client.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountMapperTest {
    private final AccountMapper mapper = new AccountMapper();

    @Test
    void shouldMapPersonalAccountBothWays() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = createClient(clientId);

        PersonalAccount account = new PersonalAccount(accountId, clientId, Instant.parse(
            "2026-10-19T10:00:00Z"),
            new AccountNumber("0000000000111111111122222222223333333333444444444455555555556666"),
            Money.pln("1250.50"), AccountStatus.ACTIVE, AccountTier.GOLD
        );

        AccountEntity entity = mapper.toEntity(account, client);

        assertInstanceOf(PersonalAccountEntity.class, entity);
        assertEquals(accountId, entity.getId());
        assertEquals(clientId, entity.getClient().getId());
        assertEquals(0, new BigDecimal("1250.50").compareTo(entity.getBalance()));

        Account restored = mapper.toDomain(entity);

        assertInstanceOf(PersonalAccount.class, restored);

        PersonalAccount personal = (PersonalAccount) restored;

        assertEquals(account.getId(), personal.getId());
        assertEquals(account.getOwnerId(), personal.getOwnerId());
        assertEquals(account.getCreatedAt(), personal.getCreatedAt());
        assertEquals(account.getAccountNumber(), personal.getAccountNumber());
        assertEquals(account.getBalance(), personal.getBalance());
        assertEquals(account.getStatus(), personal.getStatus());
        assertEquals(account.getTier(), personal.getTier());
    }

    @Test
    void shouldMapSavingsAccountBothWays() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = createClient(clientId);

        SavingsAccount account = new SavingsAccount(
            accountId, clientId,
            Instant.parse("2026-10-19T11:00:00Z"),
            new AccountNumber("0000000000111111111122222222223333333333444444444455555555556666"),
            Money.pln("5000.00"),
            AccountStatus.ACTIVE, new BigDecimal("0.045"), YearMonth.of(2026, 9)
        );

        AccountEntity entity = mapper.toEntity(account, client);

        assertInstanceOf(SavingsAccountEntity.class, entity);
        assertEquals(accountId, entity.getId());

        Account restored = mapper.toDomain(entity);

        assertInstanceOf(SavingsAccount.class, restored);

        SavingsAccount savings = (SavingsAccount) restored;

        assertEquals(account.getId(), savings.getId());
        assertEquals(account.getOwnerId(), savings.getOwnerId());
        assertEquals(account.getCreatedAt(), savings.getCreatedAt());
        assertEquals(account.getAccountNumber(), savings.getAccountNumber());
        assertEquals(account.getBalance(), savings.getBalance());
        assertEquals(account.getStatus(), savings.getStatus());
        assertEquals(0, account.getAnnualInterestRate().compareTo(savings.getAnnualInterestRate()));
        assertEquals(account.getLastInterestCalculationDate(), savings.getLastInterestCalculationDate());
    }

    private ClientEntity createClient(UUID id) {
        return new ClientEntity(
            id, "Jan", "Kowalski",
            new PersonalIdEmbeddable("12345678901"),
            new AddressEmbeddable("Polska", "Łódź", "Piotrkowska", "10", "90-001")
        );
    }
}