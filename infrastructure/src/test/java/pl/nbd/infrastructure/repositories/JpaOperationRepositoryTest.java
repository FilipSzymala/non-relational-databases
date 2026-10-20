package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.account.AccountNumberEmbeddable;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaOperationRepositoryTest {

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
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void shouldFindAllTypesOfOperationsForAccountIncludingBothTransferSides() {
        ClientEntity client = ClientEntityFactory.createClient();
        em.persist(client);

        Instant createdAt = Instant.parse("2026-10-19T09:00:00Z");
        UUID personalId = UUID.randomUUID();
        UUID savingsId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        PersonalAccountEntity personal = PersonalAccountEntity.create(personalId, client, createdAt, new AccountNumberEmbeddable("PL" + UUID.randomUUID().toString().replace("-", "")), new BigDecimal("1500.00"), AccountStatus.ACTIVE, AccountTier.GOLD);
        SavingsAccountEntity savings = SavingsAccountEntity.create(savingsId, client, createdAt, new AccountNumberEmbeddable("PL" + UUID.randomUUID().toString().replace("-", "")), new BigDecimal("5000.00"), AccountStatus.ACTIVE, new BigDecimal("0.045"), YearMonth.of(2026, 9));
        PersonalAccountEntity other = PersonalAccountEntity.create(otherId, client, createdAt, new AccountNumberEmbeddable("PL" + UUID.randomUUID().toString().replace("-", "")), new BigDecimal("200.00"), AccountStatus.ACTIVE, AccountTier.DEFAULT);
        em.persist(personal);
        em.persist(savings);
        em.persist(other);
    }

    @Test
    void shouldReturnEmptyWhenOperationDoesNotExist() {
        JpaOperationRepository repository = new JpaOperationRepository(em);
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }
}