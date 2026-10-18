package pl.nbd.bank.bank.account;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SavingsAccountTest {
    private static SavingsAccount account(String balance, String annualRate, YearMonth lastMonth) {
        return new SavingsAccount(UUID.randomUUID(), new AccountNumber(UUID.randomUUID().toString()),
                Money.pln(balance), AccountStatus.ACTIVE, new BigDecimal(annualRate), lastMonth);
    }

    @Test
    void prohibitsNegativeBalanceAndOverdraft() {
        assertThrows(IllegalArgumentException.class, () -> account("-0.01", "0.10", null));
        SavingsAccount account = account("10", "0.10", null);
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.pln("10.01")));
        assertEquals(Money.pln("10"), account.getBalance());
    }

    @Test
    void accruesOneMonthOfInterestAndUpdatesBalance() {
        SavingsAccount account = account("1200", "0.12", null);
        YearMonth month = YearMonth.of(2026, 1);
        assertEquals(Money.pln("12"), account.accrueInterest(month));
        assertEquals(Money.pln("1212"), account.getBalance());
        assertEquals(month, account.getLastInterestCalculationDate());
    }

    @Test
    void roundsMonthlyInterestToCents() {
        SavingsAccount account = account("100", "0.05", null);
        assertEquals(Money.pln("0.42"), account.accrueInterest(YearMonth.of(2026, 1)));
    }

    @Test
    void preventsCalculatingInterestTwiceForTheSameOrEarlierMonth() {
        YearMonth january = YearMonth.of(2026, 1);
        SavingsAccount account = account("100", "0.12", january);
        assertThrows(IllegalArgumentException.class, () -> account.accrueInterest(january));
        assertThrows(IllegalArgumentException.class, () -> account.accrueInterest(YearMonth.of(2025, 12)));
        assertEquals(Money.pln("100"), account.getBalance());
    }

    @Test
    void allowsInterestForLaterMonth() {
        SavingsAccount account = account("100", "0.12", YearMonth.of(2026, 1));
        account.accrueInterest(YearMonth.of(2026, 2));
        assertEquals(Money.pln("101"), account.getBalance());
        assertEquals(YearMonth.of(2026, 2), account.getLastInterestCalculationDate());
    }

    @Test
    void rejectsInvalidAnnualInterestRates() {
        assertThrows(IllegalArgumentException.class, () -> account("100", "-0.01", null));
        assertThrows(IllegalArgumentException.class, () -> account("100", "1.01", null));
    }

    @Test
    void rejectsCalculatingInterestOnBlockedAccount() {
        SavingsAccount account = account("100", "0.12", null);
        account.block();
        assertThrows(IllegalStateException.class, () -> account.accrueInterest(YearMonth.of(2026, 1)));
        assertNull(account.getLastInterestCalculationDate());
    }

    @Test
    void zeroBalanceDoesNotCreateInterestButRecordsMonth() {
        SavingsAccount account = account("0", "0.12", null);
        YearMonth month = YearMonth.of(2026, 1);
        assertEquals(Money.ZERO, account.accrueInterest(month));
        assertEquals(Money.ZERO, account.getBalance());
        assertEquals(month, account.getLastInterestCalculationDate());
    }

    @Test
    void rejectsNullCalculationMonth() {
        SavingsAccount account = account("100", "0.12", null);
        assertThrows(NullPointerException.class, () -> account.accrueInterest(null));
    }
}
