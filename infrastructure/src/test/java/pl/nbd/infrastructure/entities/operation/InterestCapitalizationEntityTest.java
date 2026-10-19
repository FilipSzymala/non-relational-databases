package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import org.hibernate.Hibernate;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterestCapitalizationEntityTest {

    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");
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

    private ClientEntity newClient() {
        return ClientEntityFactory.createClient();
    }

    private SavingsAccountEntity addSavingsAccount(ClientEntity client) {
        SavingsAccountEntity account = SavingsAccountEntity.create(UUID.randomUUID(), client, new BigDecimal("0.012"), YearMonth.of(2026, 9));
        client.addAccount(account);
        return account;
    }

    private void persistClientWithAccounts(ClientEntity client) {
        em.persist(client);
        em.flush();
    }


    @Test
    void shouldPersistAndRestoreCapitalizationAndYearMonth() {
        ClientEntity client = newClient();
        SavingsAccountEntity account = addSavingsAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        Instant time = Instant.parse("2026-10-19T01:02:03Z");
        YearMonth month = YearMonth.of(2026, 10);
        InterestCapitalizationEntity capitalization = InterestCapitalizationEntity.create(id, time, new BigDecimal("11.22"), account, month);

        em.persist(capitalization);
        em.flush();
        em.clear();

        OperationEntity found = em.find(OperationEntity.class, id);
        InterestCapitalizationEntity restored = assertInstanceOf(InterestCapitalizationEntity.class, found);
        assertEquals(id, restored.getId());
        assertEquals(time, restored.getOccurredAt());
        assertEquals(0, new BigDecimal("11.22").compareTo(restored.getAmount()));
        assertEquals(month, restored.getMonth());
        assertEquals(account.getId(), restored.getAccount().getId());
        assertEquals(SavingsAccountEntity.class, Hibernate.getClass(restored.getAccount()));
    }

    @Test
    void shouldStoreCapitalizationDiscriminator() {
        ClientEntity client = newClient();
        SavingsAccountEntity account = addSavingsAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        em.persist(InterestCapitalizationEntity.create(id, Instant.parse("2026-10-19T13:00:00Z"), new BigDecimal("12.50"), account, YearMonth.of(2026, 10)));
        em.flush();

        String type = (String) em.createNativeQuery("SELECT operation_type FROM operations WHERE id = :id").setParameter("id", id).getSingleResult();

        assertEquals("INTEREST_CAPITALIZATION", type);
    }
}
