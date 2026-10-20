package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.*;

import pl.nbd.infrastructure.entities.account.*;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaAccountRepositoryTest {

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
    void shouldFindAccountByIdNumberAndOwnerId() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        String number = "PL12345678901234567890";

        ClientEntity client =
            ClientEntityFactory.createClient(clientId);

        PersonalAccountEntity account =
            PersonalAccountEntity.create(
                accountId,
                client,
                Instant.parse("2026-10-19T10:00:00Z"),
                new AccountNumberEmbeddable(number),
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                AccountTier.GOLD
            );

        client.addAccount(account);

        em.persist(client);
        em.flush();
        em.clear();

        JpaAccountRepository repository =
            new JpaAccountRepository(em);

        Optional<AccountEntity> foundById =
            repository.findById(accountId);

        assertTrue(foundById.isPresent());
        assertEquals(accountId, foundById.get().getId());

        Optional<AccountEntity> foundByNumber =
            repository.findByAccountNumber(number);

        assertTrue(foundByNumber.isPresent());
        assertEquals(accountId, foundByNumber.get().getId());

        List<AccountEntity> foundByOwner =
            repository.findByOwnerId(clientId);

        assertEquals(1, foundByOwner.size());
        assertEquals(accountId, foundByOwner.getFirst().getId());

        assertInstanceOf(
            PersonalAccountEntity.class,
            foundById.get()
        );
    }
}
