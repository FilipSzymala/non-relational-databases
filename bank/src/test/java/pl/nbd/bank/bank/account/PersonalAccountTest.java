package pl.nbd.bank.bank.account;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersonalAccountTest {
    private static PersonalAccount account(String balance, AccountTier tier) {
        return new PersonalAccount(UUID.randomUUID(), new AccountNumber(UUID.randomUUID().toString()),
                Money.pln(balance), AccountStatus.ACTIVE, tier);
    }

    @Test
    void generatesIdAndCreationDateForNewAccount() {
        Instant before = Instant.now();
        PersonalAccount account = account("0", AccountTier.DEFAULT);
        Instant after = Instant.now();
        assertNotNull(account.getId());
        assertFalse(account.getCreatedAt().isBefore(before));
        assertFalse(account.getCreatedAt().isAfter(after));
    }

    @Test
    void creditsAndDebitsActiveAccount() {
        PersonalAccount account = account("100", AccountTier.DEFAULT);
        account.credit(Money.pln("25"));
        account.debit(Money.pln("40"));
        assertEquals(Money.pln("85"), account.getBalance());
    }

    @Test
    void rejectsZeroAndNegativeTransactionAmounts() {
        PersonalAccount account = account("100", AccountTier.DEFAULT);
        assertThrows(IllegalArgumentException.class, () -> account.credit(Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.pln("-10")));
        assertThrows(IllegalArgumentException.class, () -> account.debit(null));
        assertEquals(Money.pln("100"), account.getBalance());
    }

    @Test
    void enforcesDefaultAccountNoOverdraft() {
        PersonalAccount account = account("100", AccountTier.DEFAULT);
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.pln("100.01")));
        assertEquals(Money.pln("100"), account.getBalance());
    }

    @Test
    void enforcesSilverOverdraftLimit() {
        PersonalAccount account = account("0", AccountTier.SILVER);
        account.debit(Money.pln("500"));
        assertEquals(Money.pln("-500"), account.getBalance());
        assertThrows(IllegalArgumentException.class, () -> account.debit(Money.pln("0.01")));
        assertEquals(Money.pln("-500"), account.getBalance());
    }

    @Test
    void rejectsInitialBalanceBelowTierOverdraft() {
        assertThrows(IllegalArgumentException.class, () -> account("-500.01", AccountTier.SILVER));
        assertThrows(IllegalArgumentException.class, () -> account("-0.01", AccountTier.DEFAULT));
    }

    @Test
    void exposesDifferentOverdraftLimits() {
        assertEquals(Money.pln("0"), account("0", AccountTier.DEFAULT).getOverdraftLimit());
        assertEquals(Money.pln("500"), account("0", AccountTier.SILVER).getOverdraftLimit());
        assertEquals(Money.pln("2000"), account("0", AccountTier.GOLD).getOverdraftLimit());
        assertEquals(Money.pln("5000"), account("0", AccountTier.DIAMOND).getOverdraftLimit());
    }

    @Test
    void preventsTierDowngradeThatWouldExceedNewOverdraftLimit() {
        PersonalAccount account = account("-1000", AccountTier.GOLD);
        assertThrows(IllegalArgumentException.class, () -> account.changeAccountTier(AccountTier.SILVER));
        assertEquals(AccountTier.GOLD, account.getTier());
        account.changeAccountTier(AccountTier.DIAMOND);
        assertEquals(AccountTier.DIAMOND, account.getTier());
    }

    @Test
    void blocksAndUnblocksAccount() {
        PersonalAccount account = account("100", AccountTier.DEFAULT);
        account.block();
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
        assertThrows(IllegalStateException.class, () -> account.debit(Money.pln("1")));
        assertThrows(IllegalStateException.class, () -> account.credit(Money.pln("1")));
        assertThrows(IllegalStateException.class, account::block);
        account.unblock();
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        account.debit(Money.pln("1"));
        assertEquals(Money.pln("99"), account.getBalance());
    }

    @Test
    void rejectsUnblockingAnActiveAccount() {
        assertThrows(IllegalStateException.class, () -> account("0", AccountTier.DEFAULT).unblock());
    }

    @Test
    void closesOnlyAccountWithZeroBalance() {
        PersonalAccount account = account("100", AccountTier.DEFAULT);
        assertThrows(IllegalStateException.class, account::close);
        account.debit(Money.pln("100"));
        account.close();
        assertEquals(AccountStatus.CLOSED, account.getStatus());
        assertThrows(IllegalStateException.class, account::close);
        assertThrows(IllegalStateException.class, () -> account.credit(Money.pln("1")));
        assertThrows(IllegalStateException.class, () -> account.changeAccountTier(AccountTier.GOLD));
    }

    @Test
    void rejectsClosedAccountWithNonzeroInitialBalance() {
        assertThrows(IllegalArgumentException.class, () -> new PersonalAccount(
                UUID.randomUUID(), new AccountNumber("123"), Money.pln("5"),
                AccountStatus.CLOSED, AccountTier.DEFAULT));
    }
}
