package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AtmDepositEntityTest {

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

    private ClientEntity newClient() {
        return ClientEntityFactory.createClient();
    }

    private PersonalAccountEntity addPersonalAccount(ClientEntity client) {
        PersonalAccountEntity account = PersonalAccountEntity.create(UUID.randomUUID(), client, "GOLD");
        client.addAccount(account);
        return account;
    }

    private void persistClientWithAccounts(ClientEntity client) {
        em.persist(client);
        em.flush();
    }


    @Test
    void shouldPersistAndRestoreDepositAsConcreteSubtype() {
        ClientEntity client = newClient();
        PersonalAccountEntity account = addPersonalAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        Instant time = Instant.parse("2026-10-09T21:37:00Z");
        AtmDepositEntity deposit = AtmDepositEntity.create(id, time, new BigDecimal("420.00"), account, "Jakis-ING");

        em.persist(deposit);
        em.flush();
        em.clear();

        OperationEntity found = em.find(OperationEntity.class, id);
        AtmDepositEntity restored = assertInstanceOf(AtmDepositEntity.class, found);
        assertEquals(id, restored.getId());
        assertEquals(time, restored.getOccurredAt());
        assertEquals(0, new BigDecimal("420.00").compareTo(restored.getAmount()));
        assertEquals("Jakis-ING", restored.getAtmId());
        assertEquals(account.getId(), restored.getAccount().getId());
    }

    @Test
    void shouldStoreDepositDiscriminator() {
        ClientEntity client = newClient();
        PersonalAccountEntity account = addPersonalAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        em.persist(AtmDepositEntity.create(id, Instant.parse("2026-10-01T21:00:37Z"), new BigDecimal("67.00"), account, "PKO-BP"));
        em.flush();

        String type = (String) em.createNativeQuery("SELECT operation_type FROM operations WHERE id = :id").setParameter("id", id).getSingleResult();

        assertEquals("ATM_DEPOSIT", type);
    }
}
