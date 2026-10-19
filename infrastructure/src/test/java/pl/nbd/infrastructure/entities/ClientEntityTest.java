package pl.nbd.infrastructure.entities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.util.JpaBootstrap;

import java.util.UUID;

public class ClientEntityTest {
    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void initFactory() {
        emf = JpaBootstrap.getEntityManagerFactory();
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        em.close();
    }

    @AfterAll
    static void closeFactory() {
        emf.close();
    }

    @Test
    void persistClientEntity() {
        UUID id = UUID.randomUUID();
        ClientEntity clientEntity = new ClientEntity(id, "Kamil", "Kowalenko");
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            em.persist(clientEntity);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }

        em.contains(clientEntity);
    }
}
