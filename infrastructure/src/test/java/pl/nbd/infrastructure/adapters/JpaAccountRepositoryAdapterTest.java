package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaAccountRepositoryAdapterTest {

    private static EntityManagerFactory emf;
    private EntityManager em;
    private JpaAccountRepositoryAdapter adapter;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");
    }

    @BeforeEach
    void beforeEach() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
        adapter = new JpaAccountRepositoryAdapter(em);
    }

    @AfterEach
    void afterEach() {
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @AfterAll
    static void afterAll() {
        emf.close();
    }

    @Test
    void shouldSaveAndFindBothAccountTypes() {
        UUID ownerId = persistClient("12345678904");
        UUID personalId = UUID.randomUUID();
        UUID savingsId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-19T09:00:00Z");
        AccountNumber personalNumber = new AccountNumber("1111111111111111111111111111111111111111111111111111111111113333");
        AccountNumber savingsNumber = new AccountNumber("1111111111111111111111111111111111111111111111111111111111113333");

        PersonalAccount personal = new PersonalAccount(personalId, ownerId, createdAt, personalNumber,
            Money.pln("500.00"), AccountStatus.ACTIVE, AccountTier.GOLD);
        SavingsAccount savings = new SavingsAccount(savingsId, ownerId, createdAt, savingsNumber,
            Money.pln("1000.00"), AccountStatus.ACTIVE, new BigDecimal("0.045"),
            YearMonth.of(2026, 9));

        adapter.save(personal);
        adapter.save(savings);
        em.flush();
        em.clear();

        PersonalAccount readPersonal = assertInstanceOf(PersonalAccount.class, adapter.findById(personalId).orElseThrow());
        assertEquals(AccountTier.GOLD, readPersonal.getTier());
        assertEquals(Money.pln("500.00"), readPersonal.getBalance());
        assertEquals(personalId, adapter.findByAccountNumber(personalNumber).orElseThrow().getId());

        SavingsAccount readSavings = assertInstanceOf(SavingsAccount.class, adapter.findByAccountNumber(savingsNumber).orElseThrow());
        assertEquals(savingsId, readSavings.getId());
        assertEquals(0, new BigDecimal("0.045").compareTo(readSavings.getAnnualInterestRate()));
        assertEquals(YearMonth.of(2026, 9), readSavings.getLastInterestCalculationDate());

        assertEquals(2, adapter.findByOwnerId(ownerId).size());
    }

    @Test
    void shouldUpdateExistingBalanceStatusAndTier() {
        UUID ownerId = persistClient("12345678905");
        UUID accountId = UUID.randomUUID();
        PersonalAccount account = new PersonalAccount(accountId, ownerId, Instant.parse("2026-10-19T10:00:00Z"),
            new AccountNumber("1111111111111111111111111111111111111111111111111111111111113333"), Money.pln("1000.00"),
            AccountStatus.ACTIVE, AccountTier.GOLD);
        adapter.save(account);
        em.flush();
        em.clear();

        PersonalAccount changed = assertInstanceOf(PersonalAccount.class, adapter.findById(accountId).orElseThrow());
        changed.debit(Money.pln("250.00"));
        changed.changeAccountTier(AccountTier.SILVER);
        changed.block();
        adapter.save(changed);
        em.flush();
        em.clear();

        PersonalAccount reloaded = assertInstanceOf(PersonalAccount.class, adapter.findById(accountId).orElseThrow());
        assertEquals(Money.pln("750.00"), reloaded.getBalance());
        assertEquals(AccountStatus.BLOCKED, reloaded.getStatus());
        assertEquals(AccountTier.SILVER, reloaded.getTier());
    }

    @Test
    void shouldDeleteAccountWithoutOperations() {
        UUID ownerId = persistClient("12345678906");
        UUID accountId = UUID.randomUUID();
        Account account = new PersonalAccount(accountId, ownerId, Instant.parse("2026-10-19T11:00:00Z"),
            new AccountNumber("1111111111111111111111111111111111111111111111111111111111113333"), Money.pln("0.00"),
            AccountStatus.ACTIVE, AccountTier.DEFAULT);
        adapter.save(account);
        em.flush();
        em.clear();

        adapter.deleteById(accountId);
        em.flush();
        em.clear();

        assertTrue(adapter.findById(accountId).isEmpty());
        assertTrue(adapter.findByOwnerId(ownerId).isEmpty());
    }

    private UUID persistClient(String personalId) {
        UUID clientId = UUID.randomUUID();
        Client client = new Client(clientId, "Anna", "Nowak",
            new PersonalId(personalId), new Address("Polska", "Gdańsk", "Długa",
            "10", "1", "80-001"));
        new JpaClientRepositoryAdapter(em).save(client);
        em.flush();
        return clientId;
    }
}
