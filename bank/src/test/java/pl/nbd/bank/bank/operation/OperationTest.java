package pl.nbd.bank.bank.operation;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.shared.Money;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SOURCE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TARGET = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant TIME = Instant.parse("2025-01-01T00:00:00Z");
    private static final Money MONEY = Money.pln("10.00");
    private static final YearMonth MONTH = YearMonth.of(2025, 1);

    @Test
    void restoresAtmDepositAndRejectsInvalidReconstructedValues() {
        AtmDeposit deposit = new AtmDeposit(ID, TIME, MONEY, SOURCE, "ATM-01");
        assertEquals(ID, deposit.getId());
        assertEquals(TIME, deposit.getOccurredAt());
        assertEquals(SOURCE, deposit.getAccountId());
        assertEquals(MONEY, deposit.getAmount());
        assertEquals(Set.of(SOURCE), deposit.affectedAccounts());
        assertThrows(NullPointerException.class, () -> new AtmDeposit(ID, TIME, MONEY, null, "ATM-01"));
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(ID, TIME, MONEY, SOURCE, null));
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(ID, TIME, MONEY, SOURCE, ""));
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(MONEY, SOURCE, null));
        assertThrows(NullPointerException.class, () -> new AtmDeposit(ID, TIME, null, SOURCE, "ATM-01"));
        assertThrows(IllegalArgumentException.class, () -> new AtmDeposit(ID, TIME, Money.ZERO, SOURCE, "ATM-01"));
    }

    @Test
    void restoresAtmWithdrawAndValidatesEveryParameter() {
        AtmWithdraw withdraw = new AtmWithdraw(ID, TIME, MONEY, SOURCE, "ATM-02");
        assertEquals(ID, withdraw.getId());
        assertEquals(TIME, withdraw.getOccurredAt());
        assertEquals(SOURCE, withdraw.getAccountId());
        assertEquals("ATM-02", withdraw.getAtmId());
        assertEquals(MONEY, withdraw.getAmount());
        assertEquals(Set.of(SOURCE), withdraw.affectedAccounts());
        assertThrows(NullPointerException.class, () -> new AtmWithdraw(ID, TIME, MONEY, null, "ATM-02"));
        assertThrows(IllegalArgumentException.class, () -> new AtmWithdraw(ID, TIME, MONEY, SOURCE, ""));
        assertThrows(IllegalArgumentException.class, () -> new AtmWithdraw(ID, TIME, MONEY, SOURCE, null));
        assertThrows(IllegalArgumentException.class, () -> new AtmWithdraw(MONEY, SOURCE, null));
        assertThrows(IllegalArgumentException.class, () -> new AtmWithdraw(MONEY, SOURCE, " "));
        assertThrows(NullPointerException.class, () -> new AtmWithdraw(MONEY, null, "ATM-02"));
    }

    @Test
    void restoresTransferAndChecksBothAccounts() {
        Transfer transfer = new Transfer(ID, TIME, MONEY, SOURCE, TARGET);
        assertEquals(ID, transfer.getId());
        assertEquals(TIME, transfer.getOccurredAt());
        assertEquals(MONEY, transfer.getAmount());
        assertEquals(SOURCE, transfer.getSourceAccountId());
        assertEquals(TARGET, transfer.getTargetAccountId());
        assertEquals(Set.of(SOURCE, TARGET), transfer.affectedAccounts());
        assertThrows(NullPointerException.class, () -> new Transfer(ID, TIME, MONEY, null, TARGET));
        assertThrows(NullPointerException.class, () -> new Transfer(ID, TIME, MONEY, SOURCE, null));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(ID, TIME, MONEY, SOURCE, SOURCE));
        assertThrows(NullPointerException.class, () -> new Transfer(MONEY, null, TARGET));
        assertThrows(NullPointerException.class, () -> new Transfer(MONEY, SOURCE, null));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(MONEY, SOURCE, SOURCE));
    }

    @Test
    void restoresInterestCapitalizationAndChecksMonth() {
        InterestCapitalization interest = new InterestCapitalization(ID, TIME, MONEY, SOURCE, MONTH);
        assertEquals(ID, interest.getId());
        assertEquals(TIME, interest.getOccurredAt());
        assertEquals(MONEY, interest.getAmount());
        assertEquals(SOURCE, interest.getAccountId());
        assertEquals(MONTH, interest.getMonth());
        assertEquals(Set.of(SOURCE), interest.affectedAccounts());
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(ID, TIME, MONEY, null, MONTH));
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(ID, TIME, MONEY, SOURCE, null));
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(MONEY, null, MONTH));
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(MONEY, SOURCE, null));
    }

    @Test
    void newOperationsRejectMissingAccountsAndInvalidTimestamps() {
        assertThrows(NullPointerException.class, () -> new AtmDeposit(ID, null, MONEY, SOURCE, "ATM"));
        assertThrows(NullPointerException.class, () -> new AtmWithdraw(null, TIME, MONEY, SOURCE, "ATM"));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(ID, TIME, Money.pln("-1"), SOURCE, TARGET));
        assertThrows(NullPointerException.class, () -> new InterestCapitalization(ID, TIME, null, SOURCE, MONTH));
    }

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
