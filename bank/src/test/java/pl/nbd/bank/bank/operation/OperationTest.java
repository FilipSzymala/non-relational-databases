package pl.nbd.bank.bank.operation;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.shared.Money;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {
    @Test
    void depositUsesOnlyItsAccountAndKeepsAtmId() {
        UUID accountId = UUID.randomUUID();
        AtmDeposit deposit = new AtmDeposit(Money.pln("50"), accountId, "ATM-1");
        assertNotNull(deposit.getId());
        assertNotNull(deposit.getOccurredAt());
        assertEquals(Money.pln("50"), deposit.getAmount());
        assertEquals("ATM-1", deposit.getAtmId());
        assertEquals(Set.of(accountId), deposit.affectedAccounts());
    }

    @Test
    void withdrawalUsesOnlyItsAccount() {
        UUID accountId = UUID.randomUUID();
        AtmWithdraw withdrawal = new AtmWithdraw(Money.pln("20"), accountId, "ATM-2");
        assertEquals(Set.of(accountId), withdrawal.affectedAccounts());
    }

    @Test
    void transferAffectsTwoDistinctAccounts() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        Transfer transfer = new Transfer(Money.pln("10"), from, to);
        assertEquals(from, transfer.getSourceAccountId());
        assertEquals(to, transfer.getTargetAccountId());
        assertEquals(Set.of(from, to), transfer.affectedAccounts());
    }

    @Test
    void rejectsTransferToSameAccount() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Transfer(Money.pln("1"), id, id));
    }

    @Test
    void rejectsZeroAndNegativeOperationAmounts() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(Money.ZERO, id, "ATM-1"));
        assertThrows(IllegalArgumentException.class, () -> new AtmWithdraw(Money.pln("-1"), id, "ATM-1"));
        assertThrows(NullPointerException.class, () -> new AtmDeposit(null, id, "ATM-1"));
    }

    @Test
    void rejectsBlankAtmIdAndNullAccount() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(Money.pln("1"), id, " "));
        assertThrows(NullPointerException.class, () -> new AtmWithdraw(Money.pln("1"), null, "ATM-1"));
    }

    @Test
    void restoresOriginalOperationIdAndTimestamp() {
        UUID id = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2024-05-06T12:34:56Z");
        AtmDeposit restored = new AtmDeposit(id, occurredAt, Money.pln("100"), account, "ATM-1");
        assertEquals(id, restored.getId());
        assertEquals(occurredAt, restored.getOccurredAt());
    }

    @Test
    void rejectsNullIdAndTimestampWhenRestoringOperation() {
        UUID account = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> new AtmDeposit(
                null, Instant.now(), Money.pln("1"), account, "ATM-1"));
        assertThrows(NullPointerException.class, () -> new AtmDeposit(
                UUID.randomUUID(), null, Money.pln("1"), account, "ATM-1"));
    }

    @Test
    void recordsInterestCapitalizationMonthAndAccount() {
        UUID account = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 3);
        InterestCapitalization op = new InterestCapitalization(Money.pln("1.50"), account, month);
        assertEquals(month, op.getMonth());
        assertEquals(Set.of(account), op.affectedAccounts());
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(Money.pln("1"), account, null));
    }

    @Test
    void affectedAccountIdsAreNotMutable() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        Transfer op = new Transfer(Money.pln("10"), from, to);
        assertThrows(UnsupportedOperationException.class, () -> op.affectedAccounts().add(UUID.randomUUID()));
    }
}
