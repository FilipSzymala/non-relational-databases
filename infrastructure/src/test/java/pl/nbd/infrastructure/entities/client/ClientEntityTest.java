package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pl.nbd.infrastructure.fixtures.ClientEntityFactory.createClient;

public class ClientEntityTest {
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
    void shouldPersistClient() {
        UUID clientId = UUID.randomUUID();
        ClientEntity client = ClientEntityFactory.createClient(clientId);

        em.persist(client);
        em.flush();
        em.clear();

        ClientEntity found = em.find(ClientEntity.class, clientId);

        assertNotNull(found);
        assertEquals(clientId, found.getId());
        assertEquals("Kamil", found.getFirstName());
        assertEquals("Kowalenko", found.getLastName());
    }

    @Test
    void shouldPersistEmbeddedValues() {
        UUID clientId = UUID.randomUUID();
        ClientEntity client = createClient(clientId);

        EntityTransaction tx = em.getTransaction();

        em.persist(client);
        em.flush();
        em.clear();

        ClientEntity found = em.find(ClientEntity.class, clientId);

        assertNotNull(found);
        assertNotNull(found.getAddress());
        assertNotNull(found.getPersonalId());

        assertEquals("Płock", found.getAddress().getCity());
        assertEquals("Komendancka", found.getAddress().getStreet());
        assertEquals("1234567890", found.getPersonalId().getPersonalId());
    }

    @Test
    void shouldUpdateClient() {
        UUID clientId = UUID.randomUUID();
        ClientEntity client = createClient(clientId);

        em.persist(client);
        em.flush();
        em.clear();

        ClientEntity managed = em.find(ClientEntity.class, clientId);

        assertNotNull(managed);

        managed.setLastName("Nowak");

        em.flush();
        em.clear();

        ClientEntity updated = em.find(ClientEntity.class, clientId);

        assertNotNull(updated);
        assertEquals("Nowak", updated.getLastName());
    }

    @Test
    void shouldRemoveClient() {
        UUID clientId = UUID.randomUUID();
        ClientEntity client = createClient(clientId);

        em.persist(client);
        em.flush();
        em.clear();

        ClientEntity managed = em.find(ClientEntity.class, clientId);

        assertNotNull(managed);

        em.remove(managed);
        em.flush();
        em.clear();

        ClientEntity removed = em.find(ClientEntity.class, clientId);

        assertNull(removed);
    }


}
