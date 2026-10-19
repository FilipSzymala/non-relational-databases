package pl.nbd.infrastructure.entities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.client.AddressEmbeddable;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.entities.client.PersonalIdEmbedable;
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
        PersonalIdEmbedable personalId = new PersonalIdEmbedable("1234567890");
        AddressEmbeddable address = new AddressEmbeddable("Polska", "Płock", "Komendancka", "60", "12", "12-321");
        ClientEntity clientEntity = new ClientEntity(id, "Kamil", "Kowalenko", personalId, address);
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
