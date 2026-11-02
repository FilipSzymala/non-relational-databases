
package util;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.operation.AtmWithdraw;
import pl.nbd.bank.bank.operation.InterestCapitalization;
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.util.MongoClientFactory;
import pl.nbd.infrastructure.util.MongoDatabaseInitializer;
import pl.nbd.infrastructure.util.MongoTransactionExecutor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MongoTransactionTest {

    @Test
    void shouldAllowOnlyOneConcurrentWithdrawal() throws Exception {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoTransactionExecutor transactions = new MongoTransactionExecutor(mongoClient);
            PersonalAccount account = newAccount(UUID.randomUUID(), "100.00");

            transactions.execute(repositories -> {
                repositories.accounts().save(account);
                return null;
            });

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return withdraw(transactions, account.getId());
                });

                var second = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return withdraw(transactions, account.getId());
                });

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                int successful = (first.get(45, TimeUnit.SECONDS) ? 1 : 0) +
                    (second.get(45, TimeUnit.SECONDS) ? 1 : 0);

                assertEquals(1, successful);
            }

            transactions.execute(repositories -> {
                Account restored = repositories.accounts().findById(account.getId()).orElseThrow();
                assertEquals(Money.pln("20.00"), restored.getBalance());
                assertEquals(1, repositories.operations().findByAccountId(account.getId()).size());
                return null;
            });
        }
    }

    @Test
    void shouldRollbackAccountChangesAndOperation() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoTransactionExecutor transactions = new MongoTransactionExecutor(mongoClient);
            PersonalAccount source = newAccount(UUID.randomUUID(), "100.00");
            PersonalAccount target = newAccount(UUID.randomUUID(), "200.00");

            transactions.execute(repositories -> {
                repositories.accounts().save(source);
                repositories.accounts().save(target);
                return null;
            });

            assertThrows(IllegalStateException.class, () -> transactions.execute(repositories -> {
                Account from = repositories.accounts().findById(source.getId()).orElseThrow();
                Account to = repositories.accounts().findById(target.getId()).orElseThrow();

                Money amount = Money.pln("30.00");
                from.debit(amount);
                to.credit(amount);
                repositories.accounts().save(from);
                repositories.accounts().save(to);
                repositories.operations().save(new Transfer(amount, from.getId(), to.getId()));

                throw new IllegalStateException("Forced rollback");
            }));

            transactions.execute(repositories -> {
                assertEquals(Money.pln("100.00"), repositories.accounts().findById(source.getId()).orElseThrow().getBalance());
                assertEquals(Money.pln("200.00"), repositories.accounts().findById(target.getId()).orElseThrow().getBalance());
                assertTrue(repositories.operations().findByAccountId(source.getId()).isEmpty());
                return null;
            });
        }
    }

    @Test
    void shouldApplyTransferAtomicallyAndCapitalizeOnlyOnce() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoTransactionExecutor transactions = new MongoTransactionExecutor(mongoClient);
            PersonalAccount source = newAccount(UUID.randomUUID(), "100.00");
            SavingsAccount savings = new SavingsAccount(
                UUID.randomUUID(), new AccountNumber(accountNumber()),
                Money.pln("200.00"), AccountStatus.ACTIVE, new BigDecimal("0.12"), null
            );

            transactions.execute(repositories -> {
                repositories.accounts().save(source);
                repositories.accounts().save(savings);
                return null;
            });

            transactions.execute(repositories -> {
                Account from = repositories.accounts().findById(source.getId()).orElseThrow();
                Account to = repositories.accounts().findById(savings.getId()).orElseThrow();
                Money amount = Money.pln("30.00");
                from.debit(amount);
                to.credit(amount);
                repositories.accounts().save(from);
                repositories.accounts().save(to);
                repositories.operations().save(new Transfer(amount, source.getId(), savings.getId()));
                return null;
            });

            YearMonth month = YearMonth.of(2026, 10);
            transactions.execute(repositories -> {
                SavingsAccount account = (SavingsAccount) repositories.accounts().findById(savings.getId()).orElseThrow();
                Money interest = account.accrueInterest(month);
                repositories.accounts().save(account);

                if (interest.isPositive()) {
                    repositories.operations().save(new InterestCapitalization(interest, account.getId(), month));
                }
                return null;
            });

            assertThrows(IllegalArgumentException.class, () -> transactions.execute(repositories -> {
                SavingsAccount account = (SavingsAccount) repositories.accounts().findById(savings.getId()).orElseThrow();
                account.accrueInterest(month);
                repositories.accounts().save(account);
                return null;
            }));

            transactions.execute(repositories -> {
                assertEquals(Money.pln("70.00"), repositories.accounts().findById(source.getId()).orElseThrow().getBalance());
                assertEquals(Money.pln("232.30"), repositories.accounts().findById(savings.getId()).orElseThrow().getBalance());
                assertEquals(2, repositories.operations().findByAccountId(savings.getId()).size());
                return null;
            });
        }
    }

    private static boolean withdraw(MongoTransactionExecutor transactions, UUID accountId) {
        try {
            transactions.execute(repositories -> {
                Account account = repositories.accounts().findById(accountId).orElseThrow();
                Money amount = Money.pln("80.00");
                account.debit(amount);
                repositories.accounts().save(account);
                repositories.operations().save(new AtmWithdraw(amount, accountId, "ATM-01"));
                return null;
            });
            return true;
        } catch (IllegalArgumentException exception) {
            if (!"Insufficient funds".equals(exception.getMessage())) {
                throw exception;
            }
            return false;
        }
    }

    private static PersonalAccount newAccount(UUID ownerId, String balance) {
        return new PersonalAccount(
            ownerId,
            new AccountNumber(accountNumber()),
            Money.pln(balance),
            AccountStatus.ACTIVE,
            AccountTier.DEFAULT
        );
    }

    private static String accountNumber() {
        return UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "");
    }
}
