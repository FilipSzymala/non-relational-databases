package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.operation.AtmWithdraw;
import pl.nbd.bank.bank.operation.InterestCapitalization;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaOperationRepositoryAdapterTest {

    private static EntityManagerFactory emf;
    private EntityManager em;
    private JpaOperationRepositoryAdapter adapter;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory("POSTGRES_NBD_BANK_PU");
    }

    @BeforeEach
    void beforeEach() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
        adapter = new JpaOperationRepositoryAdapter(em);
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
    void shouldSaveAndFindAllFourOperationTypes() {
        UUID ownerId = UUID.randomUUID();
        UUID personalId = UUID.randomUUID();
        UUID savingsId = UUID.randomUUID();
        persistClientWithTwoAccounts(ownerId, personalId, savingsId, "12345678907");
        em.flush();
        em.clear();

        Instant at = Instant.parse("2026-10-19T12:00:00Z");
        AtmDeposit deposit = new AtmDeposit(
                UUID.randomUUID(), at, Money.pln("150.00"), personalId, "ATM-001");
        AtmWithdraw withdrawal = new AtmWithdraw(
                UUID.randomUUID(), at.plusSeconds(1), Money.pln("50.00"), personalId, "ATM-002");
        Transfer transfer = new Transfer(
                UUID.randomUUID(), at.plusSeconds(2), Money.pln("200.00"), personalId, savingsId);
        InterestCapitalization capitalization = new InterestCapitalization(
                UUID.randomUUID(), at.plusSeconds(3), Money.pln("3.50"), savingsId, YearMonth.of(2026, 9));

        adapter.save(deposit);
        adapter.save(withdrawal);
        adapter.save(transfer);
        adapter.save(capitalization);
        em.flush();
        em.clear();

        for (Operation original : List.of(deposit, withdrawal, transfer, capitalization)) {
            Operation restored = adapter.findById(original.getId()).orElseThrow();
            assertEquals(original.getClass(), restored.getClass());
            assertEquals(original.getOccurredAt(), restored.getOccurredAt());
            assertEquals(original.getAmount(), restored.getAmount());
            assertEquals(original.affectedAccounts(), restored.affectedAccounts());
        }
        AtmDeposit readDeposit = assertInstanceOf(
                AtmDeposit.class, adapter.findById(deposit.getId()).orElseThrow());
        assertEquals("ATM-001", readDeposit.getAtmId());
        InterestCapitalization readCapitalization = assertInstanceOf(
                InterestCapitalization.class, adapter.findById(capitalization.getId()).orElseThrow());
        assertEquals(YearMonth.of(2026, 9), readCapitalization.getMonth());

        List<Operation> personalOperations = adapter.findByAccountId(personalId);
        assertEquals(3, personalOperations.size());
        assertTrue(personalOperations.stream().anyMatch(o -> o.getId().equals(transfer.getId())));
        assertEquals(2, adapter.findByAccountId(savingsId).size());
        assertEquals(transfer.getId(), adapter.findByAccountId(savingsId).getFirst().getId());
    }

    @Test
    void shouldRejectUnknownAccountAndDuplicateOperation() {
        UUID unknownId = UUID.randomUUID();
        AtmDeposit unknown = new AtmDeposit(
                Money.pln("10.00"), unknownId, "ATM-UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> adapter.save(unknown));
        assertTrue(adapter.findById(unknown.getId()).isEmpty());

        UUID ownerId = UUID.randomUUID();
        UUID personalId = UUID.randomUUID();
        UUID savingsId = UUID.randomUUID();
        persistClientWithTwoAccounts(ownerId, personalId, savingsId, "12345678908");
        AtmDeposit valid = new AtmDeposit(Money.pln("10.00"), personalId, "ATM-003");
        adapter.save(valid);
        assertThrows(IllegalArgumentException.class, () -> adapter.save(valid));
    }

    private void persistClientWithTwoAccounts(UUID ownerId, UUID personalId, UUID savingsId, String pesel) {
        Client client = new Client(ownerId, "Tomasz", "Kowalski",
                new PersonalId(pesel),
                new Address("Polska", "Łódź", "Piotrkowska", "10", "2", "90-001"));
        Instant created = Instant.parse("2026-10-19T09:00:00Z");
        client.addAccount(new PersonalAccount(
                personalId, ownerId, created,
                accountNumber(personalId),
                Money.pln("1000.00"), AccountStatus.ACTIVE, AccountTier.GOLD
        ));
        client.addAccount(new SavingsAccount(
                savingsId, ownerId, created,
                accountNumber(savingsId),
                Money.pln("2000.00"), AccountStatus.ACTIVE,
                new BigDecimal("0.045"), YearMonth.of(2026, 8)
        ));
        new JpaClientRepositoryAdapter(em).save(client);
    }
    private static AccountNumber accountNumber(UUID id) {
        String value = "%064d".formatted(
                new java.math.BigInteger(id.toString().replace("-", ""), 16)
        );
        return new AccountNumber(value);
    }

}
