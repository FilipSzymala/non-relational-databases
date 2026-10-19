package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import org.hibernate.Hibernate;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferEntityTest {

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
        PersonalAccountEntity account = PersonalAccountEntity.create(UUID.randomUUID(), client, "GOLD");
        client.addAccount(account);
        return account;
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
    void shouldPersistTransferWithTwoDifferentAccountReferences() {
        ClientEntity client = newClient();
        PersonalAccountEntity source = addPersonalAccount(client);
        SavingsAccountEntity target = addSavingsAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        Instant time = Instant.parse("2026-10-19T12:00:00Z");
        TransferEntity transfer = TransferEntity.create(id, time, new BigDecimal("500.00"), source, target);

        em.persist(transfer);
        em.flush();
        em.clear();

        OperationEntity found = em.find(OperationEntity.class, id);
        TransferEntity restored = assertInstanceOf(TransferEntity.class, found);
        assertEquals(id, restored.getId());
        assertEquals(time, restored.getOccurredAt());
        assertEquals(0, new BigDecimal("500.00").compareTo(restored.getAmount()));
        assertEquals(source.getId(), restored.getSourceAccount().getId());
        assertEquals(target.getId(), restored.getTargetAccount().getId());
        assertEquals(PersonalAccountEntity.class, Hibernate.getClass(restored.getSourceAccount()));
        assertEquals(SavingsAccountEntity.class, Hibernate.getClass(restored.getTargetAccount()));
    }

    @Test
    void shouldStoreTransferDiscriminator() {
        ClientEntity client = newClient();
        PersonalAccountEntity source = addPersonalAccount(client);
        PersonalAccountEntity target = addPersonalAccount(client);
        persistClientWithAccounts(client);

        UUID id = UUID.randomUUID();
        em.persist(TransferEntity.create(id, Instant.parse("2026-10-19T12:00:00Z"), new BigDecimal("20.00"), source, target));
        em.flush();

        String type = (String) em.createNativeQuery("SELECT operation_type FROM operations WHERE id = :id").setParameter("id", id).getSingleResult();

        assertEquals("TRANSFER", type);
    }
}
