package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.fixtures.ClientEntityFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OperationPolymorphismTest {

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

    private ClientEntity newClient() {
        return ClientEntityFactory.createClient();
    }

    private PersonalAccountEntity addPersonalAccount(ClientEntity client) {
        PersonalAccountEntity account = PersonalAccountEntity.create(UUID.randomUUID(), client, "GOLD");
        client.addAccount(account);
        return account;
    }

    private SavingsAccountEntity addSavingsAccount(ClientEntity client) {
        SavingsAccountEntity account = SavingsAccountEntity.create(UUID.randomUUID(), client, new BigDecimal("0.012"), YearMonth.of(2026, 9));
        client.addAccount(account);
        return account;
    }

    private void persistClientWithAccounts(ClientEntity client) {
        em.persist(client);
        em.flush();
    }


    @Test
    void shouldReadAllFourTypesUsingBaseOperationQuery() {
        ClientEntity client = newClient();
        PersonalAccountEntity personal = addPersonalAccount(client);
        SavingsAccountEntity savings = addSavingsAccount(client);
        persistClientWithAccounts(client);

        Instant time = Instant.parse("2026-10-19T14:00:00Z");
        UUID depositId = UUID.randomUUID();
        UUID withdrawId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID capitalizationId = UUID.randomUUID();

        em.persist(AtmDepositEntity.create(depositId, time, new BigDecimal("10.00"), personal, "ATM-A"));
        em.persist(AtmWithdrawEntity.create(withdrawId, time, new BigDecimal("20.00"), personal, "ATM-B"));
        em.persist(TransferEntity.create(transferId, time, new BigDecimal("30.00"), personal, savings));
        em.persist(InterestCapitalizationEntity.create(capitalizationId, time, new BigDecimal("40.00"), savings, YearMonth.of(2026, 10)));

        em.flush();
        em.clear();

        List<OperationEntity> operations = em.createQuery("SELECT o FROM OperationEntity o WHERE o.id IN :ids", OperationEntity.class).setParameter("ids", List.of(depositId, withdrawId, transferId, capitalizationId)).getResultList();

        assertEquals(4, operations.size());
        assertTrue(operations.stream().anyMatch(AtmDepositEntity.class::isInstance));
        assertTrue(operations.stream().anyMatch(AtmWithdrawEntity.class::isInstance));
        assertTrue(operations.stream().anyMatch(TransferEntity.class::isInstance));
        assertTrue(operations.stream().anyMatch(InterestCapitalizationEntity.class::isInstance));
    }
}
