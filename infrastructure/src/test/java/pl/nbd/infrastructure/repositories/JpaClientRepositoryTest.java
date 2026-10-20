package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaClientRepositoryTest {

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
    void shouldPersistAndFindByIdAndPersonalId() {
        ClientEntity client = ClientEntityFactory.createClient();
        JpaClientRepository repository = new JpaClientRepository(em);

        repository.persist(client);
        em.flush();
        em.clear();

        ClientEntity byId = repository.findById(client.getId()).orElseThrow();
        assertEquals(client.getId(), byId.getId());
        assertEquals("Kamil", byId.getFirstName());
        assertEquals("Kowalenko", byId.getLastName());
        assertEquals("Płock", byId.getAddressEmbeddable().getCity());
        assertTrue(byId.getAccounts().isEmpty());

        ClientEntity byPersonalId = repository.findByPersonalId("12345678901").orElseThrow();
        assertEquals(client.getId(), byPersonalId.getId());
    }

    @Test
    void shouldReturnEmptyWhenClientDoesNotExist() {
        JpaClientRepository repository = new JpaClientRepository(em);

        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findByPersonalId("00000000000").isEmpty());
    }

    @Test
    void shouldDeleteClientWithoutAccounts() {
        ClientEntity client = ClientEntityFactory.createClient();
        JpaClientRepository repository = new JpaClientRepository(em);

        repository.persist(client);
        em.flush();

        repository.deleteById(client.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(client.getId()).isEmpty());
    }
}
