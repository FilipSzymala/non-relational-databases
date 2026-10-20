package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.operation.AtmWithdraw;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JpaAccountOptimisticLockTest {
    private static EntityManagerFactory emf;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");
    }

    @AfterAll
    static void afterAll() {
        emf.close();
    }

    @Test
    void shouldRejectSecondConcurrentWithdrawalAndRollbackItsHistory() {
        UUID accountId = createSavingsAccount();

        EntityManager firstEm = emf.createEntityManager();
        EntityManager secondEm = emf.createEntityManager();

        try {
            EntityTransaction firstTx = firstEm.getTransaction();
            EntityTransaction secondTx = secondEm.getTransaction();
            firstTx.begin();
            secondTx.begin();

            JpaAccountRepositoryAdapter firstAccounts = new JpaAccountRepositoryAdapter(firstEm);
            JpaAccountRepositoryAdapter secondAccounts = new JpaAccountRepositoryAdapter(secondEm);
            JpaOperationRepositoryAdapter firstOperations = new JpaOperationRepositoryAdapter(firstEm);
            JpaOperationRepositoryAdapter secondOperations = new JpaOperationRepositoryAdapter(secondEm);

            SavingsAccount first = assertInstanceOf(
                    SavingsAccount.class, firstAccounts.findById(accountId).orElseThrow()
            );
            SavingsAccount second = assertInstanceOf(
                    SavingsAccount.class, secondAccounts.findById(accountId).orElseThrow()
            );

            Money withdrawn = Money.pln("80.00");
            first.debit(withdrawn);
            second.debit(withdrawn);

            firstAccounts.save(first);
            firstOperations.save(new AtmWithdraw(withdrawn, accountId, "ATM-001"));
            firstTx.commit();

            RuntimeException failure = assertThrows(RuntimeException.class, () -> {
                secondAccounts.save(second);
                secondOperations.save(new AtmWithdraw(withdrawn, accountId, "ATM-002"));
                secondTx.commit();
            });
            assertTrue(hasOptimisticLockCause(failure), failure::toString);

            if (secondTx.isActive()) {
                secondTx.rollback();
            }
        } finally {
            if (firstEm.getTransaction().isActive()) {
                firstEm.getTransaction().rollback();
            }
            if (secondEm.getTransaction().isActive()) {
                secondEm.getTransaction().rollback();
            }
            firstEm.close();
            secondEm.close();
        }

        EntityManager verification = emf.createEntityManager();
        try {
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(verification);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(verification);

            SavingsAccount saved = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            assertEquals(Money.pln("20.00"), saved.getBalance());
            assertEquals(1, operations.findByAccountId(accountId).size());
        } finally {
            verification.close();
        }
    }

    @Test
    void shouldRollbackBalanceAndOperationTogether() {
        UUID accountId = createSavingsAccount();
        EntityManager em = emf.createEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();

            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(em);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(em);

            SavingsAccount account = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            Money amount = Money.pln("30.00");
            account.debit(amount);
            accounts.save(account);
            operations.save(new AtmWithdraw(amount, accountId, "ATM-003"));
            em.flush();
            tx.rollback();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        EntityManager verification = emf.createEntityManager();
        try {
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(verification);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(verification);

            SavingsAccount saved = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            assertEquals(Money.pln("100.00"), saved.getBalance());
            assertTrue(operations.findByAccountId(accountId).isEmpty());
        } finally {
            verification.close();
        }
    }

    @Test
    void shouldDetectOptimisticConflictBetweenTwoRealThreads() throws Exception {
        UUID accountId = createSavingsAccount();
        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch commitStart = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() ->
                    concurrentWithdrawal(accountId, "ATM-THREAD-1", bothReady, commitStart));
            Future<Boolean> second = executor.submit(() ->
                    concurrentWithdrawal(accountId, "ATM-THREAD-2", bothReady, commitStart));

            assertTrue(bothReady.await(30, TimeUnit.SECONDS));
            commitStart.countDown();

            boolean firstCommitted = first.get(40, TimeUnit.SECONDS);
            boolean secondCommitted = second.get(40, TimeUnit.SECONDS);
            assertNotEquals(firstCommitted, secondCommitted);
        } finally {
            commitStart.countDown();
            executor.shutdownNow();
        }

        EntityManager verification = emf.createEntityManager();
        try {
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(verification);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(verification);

            SavingsAccount saved = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            assertEquals(Money.pln("20.00"), saved.getBalance());
            assertEquals(1, operations.findByAccountId(accountId).size());
        } finally {
            verification.close();
        }
    }

    @Test
    void shouldExposeStaleDomainObjectOverwritingNewerBalance() {
        UUID accountId = createSavingsAccount();

        SavingsAccount stale;
        EntityManager staleReader = emf.createEntityManager();
        try {
            staleReader.getTransaction().begin();
            stale = assertInstanceOf(
                    SavingsAccount.class,
                    new JpaAccountRepositoryAdapter(staleReader).findById(accountId).orElseThrow()
            );
            staleReader.getTransaction().commit();
        } finally {
            if (staleReader.getTransaction().isActive()) {
                staleReader.getTransaction().rollback();
            }
            staleReader.close();
        }

        EntityManager currentWriter = emf.createEntityManager();
        try {
            currentWriter.getTransaction().begin();
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(currentWriter);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(currentWriter);

            SavingsAccount current = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            current.debit(Money.pln("30.00"));
            accounts.save(current);
            operations.save(new AtmWithdraw(Money.pln("30.00"), accountId, "ATM-CURRENT"));
            currentWriter.getTransaction().commit();
        } finally {
            if (currentWriter.getTransaction().isActive()) {
                currentWriter.getTransaction().rollback();
            }
            currentWriter.close();
        }

        stale.debit(Money.pln("80.00"));
        EntityManager staleWriter = emf.createEntityManager();
        try {
            staleWriter.getTransaction().begin();
            new JpaAccountRepositoryAdapter(staleWriter).save(stale);
            new JpaOperationRepositoryAdapter(staleWriter).save(
                    new AtmWithdraw(Money.pln("80.00"), accountId, "ATM-STALE")
            );
            staleWriter.getTransaction().commit();
        } finally {
            if (staleWriter.getTransaction().isActive()) {
                staleWriter.getTransaction().rollback();
            }
            staleWriter.close();
        }

        EntityManager verification = emf.createEntityManager();
        try {
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(verification);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(verification);

            SavingsAccount saved = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            assertEquals(Money.pln("20.00"), saved.getBalance());
            assertEquals(2, operations.findByAccountId(accountId).size());
            Money totalWithdrawn = operations.findByAccountId(accountId).stream()
                    .map(operation -> operation.getAmount())
                    .reduce(Money.ZERO, Money::add);
            assertEquals(Money.pln("110.00"), totalWithdrawn);
        } finally {
            verification.close();
        }
    }

    private boolean concurrentWithdrawal(
            UUID accountId,
            String atmId,
            CountDownLatch bothReady,
            CountDownLatch commitStart
    ) throws InterruptedException {
        EntityManager em = emf.createEntityManager();
        boolean readySignalled = false;

        try {
            em.getTransaction().begin();
            JpaAccountRepositoryAdapter accounts = new JpaAccountRepositoryAdapter(em);
            JpaOperationRepositoryAdapter operations = new JpaOperationRepositoryAdapter(em);

            SavingsAccount account = assertInstanceOf(
                    SavingsAccount.class, accounts.findById(accountId).orElseThrow()
            );
            Money amount = Money.pln("80.00");
            account.debit(amount);
            accounts.save(account);
            operations.save(new AtmWithdraw(amount, accountId, atmId));

            bothReady.countDown();
            readySignalled = true;
            if (!commitStart.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent commits");
            }

            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (hasOptimisticLockCause(e)) {
                return false;
            }
            throw e;
        } finally {
            if (!readySignalled) {
                bothReady.countDown();
            }
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private UUID createSavingsAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String personalId = "%011d".formatted(
                new BigInteger(clientId.toString().replace("-", ""), 16)
                        .mod(BigInteger.valueOf(100_000_000_000L))
        );
        String accountNumber = "%064d".formatted(
                new BigInteger(accountId.toString().replace("-", ""), 16)
        );

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            new JpaClientRepositoryAdapter(em).save(new Client(
                    clientId,
                    "Anna",
                    "Nowak",
                    new PersonalId(personalId),
                    new Address("Polska", "Łódź", "Długa", "10", "1", "90-001")
            ));
            new JpaAccountRepositoryAdapter(em).save(new SavingsAccount(
                    accountId,
                    clientId,
                    Instant.parse("2026-10-19T10:00:00Z"),
                    new AccountNumber(accountNumber),
                    Money.pln("100.00"),
                    AccountStatus.ACTIVE,
                    new BigDecimal("0.045"),
                    YearMonth.of(2026, 9)
            ));
            em.getTransaction().commit();
            return accountId;
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private static boolean hasOptimisticLockCause(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
