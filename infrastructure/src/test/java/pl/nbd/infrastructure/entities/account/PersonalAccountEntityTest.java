package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

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
    void shouldPersistPersonalAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        ClientEntity client = ClientEntityFactory.createClient(clientId);

        PersonalAccountEntity account = PersonalAccountEntity.create(accountId, client, "GOLD");

        client.addAccount(account);

        em.persist(client);
        em.flush();
        em.clear();

        AccountEntity found = em.find(AccountEntity.class, accountId);

        assertNotNull(found);
        assertInstanceOf(PersonalAccountEntity.class, found);

        PersonalAccountEntity personal = (PersonalAccountEntity) found;

        assertEquals(accountId, personal.getId());
        assertEquals("GOLD", personal.getTier());
        assertEquals(clientId, personal.getClient().getId());
    }
}
