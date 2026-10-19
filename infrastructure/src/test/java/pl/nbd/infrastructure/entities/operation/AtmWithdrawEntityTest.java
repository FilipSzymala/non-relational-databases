package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import pl.nbd.infrastructure.entities.account.AccountNumberEmbeddable;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AtmWithdrawEntityTest {

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

    private PersonalAccountEntity addPersonalAccount(ClientEntity client) {
        UUID accountId = UUID.randomUUID();
        PersonalAccountEntity account = PersonalAccountEntity.create(
            accountId, client,
            Instant.parse("2026-10-19T10:00:00Z"),
            new AccountNumberEmbeddable("PL" + accountId.toString().replace("-", "")),
            new BigDecimal("1000.00"), AccountStatus.ACTIVE, AccountTier.GOLD
        );
        client.addAccount(account);
        return account;
    }

    private void persistClientWithAccounts(ClientEntity client) {
        em.persist(client);
        em.flush();
    }


    @Test
    void shouldPersistAndRestoreWithdrawalAsConcreteSubtype() {
        ClientEntity client = newClient();
        PersonalAccountEntity account = addPersonalAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        Instant time = Instant.parse("2022-02-21T22:11:33Z");
        AtmWithdrawEntity withdraw = AtmWithdrawEntity.create(id, time, new BigDecimal("120.50"), account, "BanKOMat");

        em.persist(withdraw);
        em.flush();
        em.clear();

        OperationEntity found = em.find(OperationEntity.class, id);
        AtmWithdrawEntity restored = assertInstanceOf(AtmWithdrawEntity.class, found);
        assertEquals(id, restored.getId());
        assertEquals(time, restored.getOccurredAt());
        assertEquals(0, new BigDecimal("120.50").compareTo(restored.getAmount()));
        assertEquals("BanKOMat", restored.getAtmId());
        assertEquals(account.getId(), restored.getAccount().getId());
    }

    @Test
    void shouldStoreWithdrawalDiscriminator() {
        ClientEntity client = newClient();
        PersonalAccountEntity account = addPersonalAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        em.persist(AtmWithdrawEntity.create(id, Instant.parse("2026-10-19T11:00:00Z"), new BigDecimal("10.00"), account, "ABC-def"));
        em.flush();

        String type = (String) em.createNativeQuery("SELECT operation_type FROM operations WHERE id = :id").setParameter("id", id).getSingleResult();

        assertEquals("ATM_WITHDRAW", type);
    }
}
