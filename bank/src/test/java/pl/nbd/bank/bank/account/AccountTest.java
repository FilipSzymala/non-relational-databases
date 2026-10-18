package pl.nbd.bank.bank.account;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant TIME = Instant.parse("2024-03-01T10:20:30Z");
    private static final AccountNumber NUMBER = new AccountNumber("12345678901234567890123456");

    private static PersonalAccount restored(UUID id, UUID owner, Instant createdAt, AccountNumber number,
                                             Money balance, AccountStatus status, AccountTier tier) {
        return new PersonalAccount(id, owner, createdAt, number, balance, status, tier);
    }

    @Test
    void recreatesSamePersonalAccountIdentityAndState() {
        PersonalAccount account = restored(ID, OWNER, TIME, NUMBER, Money.pln("-15.00"),
                AccountStatus.BLOCKED, AccountTier.SILVER);
        assertEquals(ID, account.getId());
        assertEquals(OWNER, account.getOwnerId());
        assertEquals(TIME, account.getCreatedAt());
        assertEquals(NUMBER, account.getAccountNumber());
        assertEquals(Money.pln("-15.00"), account.getBalance());
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
        assertEquals(AccountTier.SILVER, account.getTier());
        account.unblock();
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void restoredAccountRejectsMissingIdentityFields() {
        assertThrows(NullPointerException.class, () -> restored(null, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, null, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, OWNER, null, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, OWNER, TIME, null, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, OWNER, TIME, NUMBER, null, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, OWNER, TIME, NUMBER, Money.ZERO, null, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> restored(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, null));
    }

    @Test
    void newlyCreatedPersonalAccountRejectsMissingFields() {
        assertThrows(NullPointerException.class, () -> new PersonalAccount(null, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> new PersonalAccount(OWNER, null, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> new PersonalAccount(OWNER, NUMBER, null, AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> new PersonalAccount(OWNER, NUMBER, Money.ZERO, null, AccountTier.DEFAULT));
        assertThrows(NullPointerException.class, () -> new PersonalAccount(OWNER, NUMBER, Money.ZERO, AccountStatus.ACTIVE, null));
    }

    @Test
    void initialStateValidationRejectsInvalidRestoredValues() {
        assertThrows(IllegalArgumentException.class, () -> restored(ID, OWNER, TIME, NUMBER, Money.pln("-1"), AccountStatus.ACTIVE, AccountTier.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> restored(ID, OWNER, TIME, NUMBER, Money.pln("1"), AccountStatus.CLOSED, AccountTier.DEFAULT));
        assertEquals(AccountStatus.CLOSED, restored(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.CLOSED, AccountTier.DEFAULT).getStatus());
    }

    @Test
    void creditDebitAndTierEdgeCases() {
        PersonalAccount account = restored(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.GOLD);
        assertThrows(IllegalArgumentException.class, () -> account.credit(null));
        assertThrows(IllegalArgumentException.class, () -> account.credit(Money.pln("-1")));
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.ZERO));
        assertThrows(NullPointerException.class, () -> account.changeAccountTier(null));
        account.debit(Money.pln("2000"));
        assertThrows(IllegalArgumentException.class, () -> account.changeAccountTier(AccountTier.DEFAULT));
        account.changeAccountTier(AccountTier.DIAMOND);
        assertEquals(AccountTier.DIAMOND, account.getTier());
    }

    @Test
    void closedAndBlockedAccountsObeyStateTransitions() {
        PersonalAccount account = restored(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT);
        account.block();
        account.close();
        assertEquals(AccountStatus.CLOSED, account.getStatus());
        assertThrows(IllegalStateException.class, account::block);
        assertThrows(IllegalStateException.class, account::unblock);
        assertThrows(IllegalStateException.class, () -> account.debit(Money.pln("1")));
    }

    private static SavingsAccount savings(UUID id, UUID owner, Instant createdAt, AccountNumber number,
                                          Money balance, AccountStatus status, BigDecimal rate, YearMonth last) {
        return new SavingsAccount(id, owner, createdAt, number, balance, status, rate, last);
    }

    @Test
    void recreatesSavingsAccountWithHistoricalDatesAndRate() {
        YearMonth last = YearMonth.of(2024, 2);
        SavingsAccount account = savings(ID, OWNER, TIME, NUMBER, Money.pln("1000"), AccountStatus.BLOCKED,
                new BigDecimal("0.09"), last);
        assertEquals(ID, account.getId());
        assertEquals(TIME, account.getCreatedAt());
        assertEquals(OWNER, account.getOwnerId());
        assertEquals(NUMBER, account.getAccountNumber());
        assertEquals(new BigDecimal("0.09"), account.getAnnualInterestRate());
        assertEquals(last, account.getLastInterestCalculationDate());
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
    }

    @Test
    void restoredSavingsAccountRejectsInvalidRateAndBalance() {
        assertThrows(NullPointerException.class, () -> savings(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, null, null));
        assertThrows(IllegalArgumentException.class, () -> savings(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, new BigDecimal("-0.1"), null));
        assertThrows(IllegalArgumentException.class, () -> savings(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.ACTIVE, new BigDecimal("1.1"), null));
        assertThrows(IllegalArgumentException.class, () -> savings(ID, OWNER, TIME, NUMBER, Money.pln("-1"), AccountStatus.ACTIVE, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> savings(ID, OWNER, TIME, NUMBER, Money.pln("1"), AccountStatus.CLOSED, BigDecimal.ZERO, null));
        assertEquals(Money.ZERO, savings(ID, OWNER, TIME, NUMBER, Money.ZERO, AccountStatus.CLOSED, BigDecimal.ZERO, null).getBalance());
    }

    @Test
    void newSavingsAccountRejectsNullRate() {
        assertThrows(NullPointerException.class, () -> new SavingsAccount(OWNER, NUMBER, Money.ZERO, AccountStatus.ACTIVE, null, null));
    }

    @Test
    void accountNumberMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new AccountNumber(null));
        assertThrows(IllegalArgumentException.class, () -> new AccountNumber("  "));
        assertEquals("ABC", new AccountNumber("ABC").number());
    }
}
