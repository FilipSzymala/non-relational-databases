package pl.nbd.infrastructure.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaBootstrap {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
