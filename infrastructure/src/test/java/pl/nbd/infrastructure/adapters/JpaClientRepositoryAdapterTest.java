package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.shared.Money;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaClientRepositoryAdapterTest {

    private static EntityManagerFactory emf;
    private EntityManager em;
    private JpaClientRepositoryAdapter adapter;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");
    }

    @BeforeEach
    void beforeEach() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
        adapter = new JpaClientRepositoryAdapter(em);
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
    void shouldSaveAndRestoreClientWithAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        PersonalId personalId = new PersonalId("12345678901");

        Client client = createClient(clientId, personalId);
        client.addAccount(new PersonalAccount(
                accountId, clientId, java.time.Instant.parse("2026-10-19T10:00:00Z"),
                accountNumber(accountId),
                Money.pln("1250.50"), AccountStatus.ACTIVE, AccountTier.GOLD
        ));

        adapter.save(client);
        em.flush();
        em.clear();

        Client byId = adapter.findById(clientId).orElseThrow();
        assertEquals(client.getFirstName(), byId.getFirstName());
        assertEquals(client.getLastName(), byId.getLastName());
        assertEquals(client.getPersonalId(), byId.getPersonalId());
        assertEquals(client.getAddress(), byId.getAddress());
        assertEquals(1, byId.getAccounts().size());
        assertEquals(accountId, byId.getAccounts().getFirst().getId());
        assertEquals(Money.pln("1250.50"), byId.getAccounts().getFirst().getBalance());

        Client byPersonalId = adapter.findByPersonalId(personalId).orElseThrow();
        assertEquals(clientId, byPersonalId.getId());
        assertEquals(1, byPersonalId.getAccounts().size());
    }

    @Test
    void shouldUpdateClientAndAddNewAccount() {
        UUID clientId = UUID.randomUUID();
        Client client = createClient(clientId, new PersonalId("12345678902"));
        adapter.save(client);
        em.flush();
        em.clear();

        Client restored = adapter.findById(clientId).orElseThrow();
        restored.setFirstName("Anna");
        restored.setAddress(new Address(
                "Polska", "Warszawa", "Nowa", "22", "4", "00-001"
        ));
        UUID accountId = UUID.randomUUID();
        restored.addAccount(new PersonalAccount(
                accountId, clientId, java.time.Instant.parse("2026-10-19T11:00:00Z"),
                accountNumber(accountId),
                Money.pln("200.00"), AccountStatus.ACTIVE, AccountTier.SILVER
        ));

        adapter.save(restored);
        em.flush();
        em.clear();

        Client updated = adapter.findById(clientId).orElseThrow();
        assertEquals("Anna", updated.getFirstName());
        assertEquals("Warszawa", updated.getAddress().city());
        assertEquals(1, updated.getAccounts().size());
        assertEquals(accountId, updated.getAccounts().getFirst().getId());
    }

    @Test
    void shouldUpdateExistingAccountThroughClientSave() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Client client = createClient(clientId, new PersonalId("12345678909"));
        client.addAccount(new PersonalAccount(
                accountId, clientId, java.time.Instant.parse("2026-10-19T10:00:00Z"),
                accountNumber(accountId),
                Money.pln("1000.00"), AccountStatus.ACTIVE, AccountTier.GOLD
        ));
        adapter.save(client);
        em.flush();
        em.clear();

        Client changed = adapter.findById(clientId).orElseThrow();
        PersonalAccount account = assertInstanceOf(
                PersonalAccount.class, changed.getAccounts().getFirst());
        account.debit(Money.pln("250.00"));
        account.changeAccountTier(AccountTier.SILVER);
        adapter.save(changed);
        em.flush();
        em.clear();

        Client loaded = adapter.findById(clientId).orElseThrow();
        assertEquals(1, loaded.getAccounts().size());
        PersonalAccount updated = assertInstanceOf(
                PersonalAccount.class, loaded.getAccounts().getFirst());
        assertEquals(Money.pln("750.00"), updated.getBalance());
        assertEquals(AccountTier.SILVER, updated.getTier());
    }

    @Test
    void shouldDeleteClientWithoutAccountsAndReturnEmpty() {
        UUID id = UUID.randomUUID();
        Client client = createClient(id, new PersonalId("12345678903"));
        adapter.save(client);
        em.flush();
        em.clear();

        adapter.deleteById(id);
        em.flush();
        em.clear();

        assertTrue(adapter.findById(id).isEmpty());
        assertTrue(adapter.findByPersonalId(client.getPersonalId()).isEmpty());
    }

    private static Client createClient(UUID id, PersonalId personalId) {
        return new Client(id, "Kamil", "Kowalenko", personalId,
                new Address("Polska", "Płock", "Komendancka", "60", "12", "12-321"));
    }
    private static AccountNumber accountNumber(UUID id) {
        String value = "%064d".formatted(
                new java.math.BigInteger(id.toString().replace("-", ""), 16)
        );
        return new AccountNumber(value);
    }

}
