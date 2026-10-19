package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PersonalAccountEntityTest {
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
                throw e;
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
    void shouldPersistPersonalAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = ClientEntityFactory.createClient(clientId);

        PersonalAccountEntity account = PersonalAccountEntity.create(
            accountId, client,
            Instant.parse("2026-10-19T10:00:00Z"),
            new AccountNumberEmbeddable("PL" + accountId.toString().replace("-", "")),
            new BigDecimal("1000.00"), AccountStatus.ACTIVE, AccountTier.GOLD
        );

        client.addAccount(account);

        em.persist(client);
        em.flush();
        em.clear();

        AccountEntity found = em.find(AccountEntity.class, accountId);

        assertNotNull(found);
        assertInstanceOf(PersonalAccountEntity.class, found);

        PersonalAccountEntity personal = (PersonalAccountEntity) found;

        assertEquals(accountId, personal.getId());
        assertEquals(Instant.parse("2026-10-19T10:00:00Z"), personal.getCreatedAt());
        assertEquals("PL" + accountId.toString().replace("-", ""),
                personal.getAccountNumber().getAccountNumber());
        assertEquals(0, new BigDecimal("1000.00").compareTo(personal.getBalance()));
        assertEquals(AccountStatus.ACTIVE, personal.getStatus());
        assertEquals(AccountTier.GOLD, personal.getTier());
        assertEquals(clientId, personal.getClient().getId());
    }
}
