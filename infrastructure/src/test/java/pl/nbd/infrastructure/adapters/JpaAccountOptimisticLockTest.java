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
