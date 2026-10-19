package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountEntityTest {
    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory(
            "POSTGRES_NBD_BANK_PU"
        );
    }

    @BeforeEach
    void beforeEach() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    @AfterEach
    void afterEach() {
        if (em != null && em.isOpen()) {
            try {
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } finally {
                em.close();
            }
        }
    }

    @AfterAll
    static void afterAll() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void shouldPersistAndFindAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = ClientEntityFactory.createClient(clientId);
        AccountEntity account = PersonalAccountEntity.create(accountId, client, "GOLD");

        em.persist(client);
        em.persist(account);

        em.flush();
        em.clear();

        AccountEntity found = em.find(AccountEntity.class, accountId);

        assertNotNull(found);
        assertNotSame(account, found);
        assertTrue(em.contains(found));
    }

    @Test
    void shouldPersistTwoAccountsUsingCascade() {
        UUID clientId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        BigDecimal interestRate = new BigDecimal("0.042");

        ClientEntity client = ClientEntityFactory.createClient(clientId);

        PersonalAccountEntity account1 = PersonalAccountEntity.create(accountId1, client, "GOLD");
        AccountEntity account2 = SavingsAccountEntity.create(accountId2, client, interestRate, YearMonth.of(2023, 1));

        client.addAccount(account1);
        client.addAccount(account2);

        em.persist(client);

        em.flush();
        em.clear();

        ClientEntity foundClient =
            em.find(ClientEntity.class, clientId);

        assertNotNull(foundClient);
        assertEquals(2, foundClient.getAccounts().size());

        assertNotNull(em.find(AccountEntity.class, accountId1));
        assertNotNull(em.find(AccountEntity.class, accountId2));
    }

    @Test
    void shouldPersistAccountWithCorrectClient() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = ClientEntityFactory.createClient(clientId);
        AccountEntity account = PersonalAccountEntity.create(accountId, client, "GOLD");

        em.persist(client);
        em.persist(account);

        em.flush();
        em.clear();

        UUID actualClientId = em.createQuery("""
                SELECT a.client.id
                FROM AccountEntity a
                WHERE a.id = :accountId
                """, UUID.class)
            .setParameter("accountId", accountId)
            .getSingleResult();

        assertEquals(clientId, actualClientId);
    }

    @Test
    void shouldRemoveAccountWithoutRemovingClient() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = ClientEntityFactory.createClient(clientId);
        AccountEntity account = PersonalAccountEntity.create(accountId, client, "GOLD");

        em.persist(client);
        em.persist(account);

        em.flush();
        em.clear();

        AccountEntity managedAccount =
            em.find(AccountEntity.class, accountId);

        assertNotNull(managedAccount);

        em.remove(managedAccount);

        em.flush();
        em.clear();

        assertNull(em.find(AccountEntity.class, accountId));
        assertNotNull(em.find(ClientEntity.class, clientId));
    }


    @Test
    void shouldReturnNullForNonExistingAccount() {
        UUID nonExistingId = UUID.randomUUID();

        AccountEntity found =
            em.find(AccountEntity.class, nonExistingId);

        assertNull(found);
    }
}