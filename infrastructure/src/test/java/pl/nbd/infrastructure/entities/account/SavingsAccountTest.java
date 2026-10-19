package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SavingsAccountTest {
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
    void shouldPersistSavingsAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        BigDecimal interestRate = new BigDecimal("0.045");
        YearMonth calculationDate = YearMonth.of(2026, 10);

        ClientEntity client = ClientEntityFactory.createClient(clientId);

        SavingsAccountEntity account = SavingsAccountEntity.create(accountId, client, interestRate, calculationDate);

        client.addAccount(account);

        em.persist(client);
        em.flush();
        em.clear();

        AccountEntity found = em.find(AccountEntity.class, accountId);

        assertNotNull(found);
        assertInstanceOf(SavingsAccountEntity.class, found);

        SavingsAccountEntity savings = (SavingsAccountEntity) found;

        assertEquals(accountId, savings.getId());

        assertEquals(0, interestRate.compareTo(savings.getAnnualInterestRate()));

        assertEquals(calculationDate, savings.getLastInterestCalculationDate());

        assertEquals(clientId, savings.getClient().getId());
    }
}
