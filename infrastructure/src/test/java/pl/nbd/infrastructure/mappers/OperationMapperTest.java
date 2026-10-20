package pl.nbd.infrastructure.mappers;

import org.junit.jupiter.api.Test;

import pl.nbd.bank.bank.operation.*;
import pl.nbd.bank.bank.shared.Money;

import pl.nbd.infrastructure.entities.account.*;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.entities.operation.*;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OperationMapperTest {
    private final OperationMapper mapper = new OperationMapper();

    @Test
    void shouldMapAllOperationTypesBothWays() {
        ClientEntity client = ClientEntityFactory.createClient();

        UUID personalId = UUID.randomUUID();
        UUID savingsId = UUID.randomUUID();

        Instant createdAt = Instant.parse("2026-10-19T10:00:00Z");

        PersonalAccountEntity personal = PersonalAccountEntity.create(
            personalId, client, createdAt,
            new AccountNumberEmbeddable("0000000000111111111122222222223333333333444444444455555555556666"),
            new BigDecimal("1000.00"),
            AccountStatus.ACTIVE,
            AccountTier.GOLD
        );

        SavingsAccountEntity savings = SavingsAccountEntity.create(
            savingsId, client, createdAt,
            new AccountNumberEmbeddable("0000000000111111111122222222223333333333444444444455555555556666"),
            new BigDecimal("5000.00"), AccountStatus.ACTIVE,
            new BigDecimal("0.045"),
            YearMonth.of(2026, 9)
        );

        Map<UUID, AccountEntity> accounts = Map.of(personalId, personal, savingsId, savings);

        Instant occurredAt = Instant.parse("2026-10-19T12:00:00Z");

        Money amount = Money.pln("250.00");

        AtmDeposit deposit = new AtmDeposit(UUID.randomUUID(), occurredAt, amount, personalId, "ATM-001");

        AtmWithdraw withdraw = new AtmWithdraw(UUID.randomUUID(), occurredAt, amount, personalId, "ATM-002");

        Transfer transfer = new Transfer(UUID.randomUUID(), occurredAt, amount, personalId, savingsId);

        InterestCapitalization capitalization = new InterestCapitalization(
            UUID.randomUUID(), occurredAt, amount, savingsId,
            YearMonth.of(2026, 9)
        );

        for (Operation original : new Operation[]{deposit, withdraw, transfer, capitalization}) {
            OperationEntity entity = mapper.toEntity(original, accounts);

            Operation restored = mapper.toDomain(entity);

            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getOccurredAt(), restored.getOccurredAt());
            assertEquals(original.getAmount(), restored.getAmount());
            assertEquals(original.affectedAccounts(), restored.affectedAccounts());
            assertEquals(original.getClass(), restored.getClass());
        }

        AtmDepositEntity depositEntity = assertInstanceOf(
            AtmDepositEntity.class, mapper.toEntity(deposit, accounts)
        );

        assertEquals("ATM-001", depositEntity.getAtmId());
        assertSame(personal, depositEntity.getAccount());

        AtmWithdrawEntity withdrawEntity = assertInstanceOf(
            AtmWithdrawEntity.class, mapper.toEntity(withdraw, accounts)
        );

        assertEquals("ATM-002", withdrawEntity.getAtmId());

        TransferEntity transferEntity = assertInstanceOf(
            TransferEntity.class, mapper.toEntity(transfer, accounts)
        );

        assertSame(personal, transferEntity.getSourceAccount());
        assertSame(savings, transferEntity.getTargetAccount());
        assertNull(transferEntity.getAccount());

        InterestCapitalizationEntity capitalizationEntity = assertInstanceOf(
            InterestCapitalizationEntity.class, mapper.toEntity(capitalization, accounts)
        );

        assertSame(savings, capitalizationEntity.getAccount());
        assertEquals(
            YearMonth.of(2026, 9), capitalizationEntity.getMonth()
        );
    }
}
