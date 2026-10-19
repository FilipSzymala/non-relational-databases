package pl.nbd.infrastructure.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JpaBootstrapTest {

    @Test
    void shouldCreateEntityManager() {
        EntityManagerFactory emf = JpaBootstrap.getEntityManagerFactory();

        assertNotNull(emf);
        assertTrue(emf.isOpen());

        try (EntityManager em = emf.createEntityManager()) {
            assertTrue(em.isOpen());
        }
    }
}
