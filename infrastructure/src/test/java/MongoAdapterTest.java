import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
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
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.bank.repository.AccountRepository;
import pl.nbd.bank.repository.ClientRepository;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.adapters.MongoAccountRepositoryAdapter;
import pl.nbd.infrastructure.adapters.MongoClientRepositoryAdapter;
import pl.nbd.infrastructure.adapters.MongoOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.mappers.ClientMapper;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;
import pl.nbd.infrastructure.repositories.MongoClientRepository;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;
import pl.nbd.infrastructure.util.MongoClientFactory;
import pl.nbd.infrastructure.util.MongoDatabaseInitializer;
import pl.nbd.infrastructure.util.MongoTransactionExecutor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MongoAdapterTest {

    @Test
    void shouldSaveAndRestoreClientWithBothAccountTypes() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();

            ClientRepository clients = new MongoClientRepositoryAdapter(
                new MongoClientRepository(mongoClient), new MongoAccountRepository(mongoClient),
                new ClientMapper(), new AccountMapper()
            );

            Client client = newClient();
            PersonalAccount personal = newPersonalAccount(client.getId(), "100.00");
            SavingsAccount savings = new SavingsAccount(
                client.getId(), new AccountNumber(accountNumber()), Money.pln("500.00"),
                AccountStatus.ACTIVE, new BigDecimal("0.05"), null
            );

            client.addAccount(personal);
            client.addAccount(savings);
            clients.save(client);

            Client byId = clients.findById(client.getId()).orElseThrow();
            Client byPersonalId = clients.findByPersonalId(client.getPersonalId()).orElseThrow();

            assertEquals(client.getId(), byId.getId());
            assertEquals(client.getId(), byPersonalId.getId());
            assertEquals("Anna", byId.getFirstName());
            assertEquals("Testowa", byId.getAddress().street());
            assertEquals(2, byId.getAccounts().size());
            assertTrue(byId.getAccounts().stream().anyMatch(account -> account instanceof PersonalAccount));
            assertTrue(byId.getAccounts().stream().anyMatch(account -> account instanceof SavingsAccount));
            assertTrue(byId.getAccounts().stream().anyMatch(account -> account.getId().equals(personal.getId())));
            assertTrue(byId.getAccounts().stream().anyMatch(account -> account.getId().equals(savings.getId())));
            assertThrows(IllegalStateException.class, () -> clients.deleteById(client.getId()));
        }
    }

    @Test
    void shouldSaveFindAndDeleteAccountThroughDomainInterface() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            AccountRepository accounts = new MongoAccountRepositoryAdapter(
                new MongoAccountRepository(mongoClient), new AccountMapper()
            );

            PersonalAccount account = newPersonalAccount(UUID.randomUUID(), "120.00");
            accounts.save(account);

            Account byId = accounts.findById(account.getId()).orElseThrow();
            Account byNumber = accounts.findByAccountNumber(account.getAccountNumber()).orElseThrow();

            assertInstanceOf(PersonalAccount.class, byId);
            assertEquals(account.getId(), byNumber.getId());
            assertEquals(account.getOwnerId(), byId.getOwnerId());
            assertEquals(account.getBalance(), byId.getBalance());
            assertEquals(AccountTier.GOLD, ((PersonalAccount) byId).getTier());
            assertEquals(1, accounts.findByOwnerId(account.getOwnerId()).size());

            accounts.deleteById(account.getId());
            assertTrue(accounts.findById(account.getId()).isEmpty());
        }
    }

    @Test
    void shouldSaveAndRestoreAllOperationTypesThroughDomainInterface() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            OperationRepository operations = new MongoOperationRepositoryAdapter(
                new MongoOperationRepository(mongoClient), new OperationMapper()
            );
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            AtmDeposit deposit = new AtmDeposit(Money.pln("100.00"), sourceId, "ATM-01");
            AtmWithdraw withdrawal = new AtmWithdraw(Money.pln("20.00"), sourceId, "ATM-02");
            Transfer transfer = new Transfer(Money.pln("30.00"), sourceId, targetId);
            InterestCapitalization interest = new InterestCapitalization(
                Money.pln("2.00"), sourceId, YearMonth.of(2026, 10)
            );

            operations.save(deposit);
            operations.save(withdrawal);
            operations.save(transfer);
            operations.save(interest);

            assertInstanceOf(AtmDeposit.class, operations.findById(deposit.getId()).orElseThrow());
            assertInstanceOf(AtmWithdraw.class, operations.findById(withdrawal.getId()).orElseThrow());
            assertInstanceOf(Transfer.class, operations.findById(transfer.getId()).orElseThrow());
            assertInstanceOf(InterestCapitalization.class, operations.findById(interest.getId()).orElseThrow());
            assertEquals(transfer.getAmount(), operations.findById(transfer.getId()).orElseThrow().getAmount());
            assertEquals(4, operations.findByAccountId(sourceId).size());
            assertEquals(1, operations.findByAccountId(targetId).size());
        }
    }

    @Test
    void shouldRejectStaleAccountAndPersistTransactionUpdate() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            AccountRepository accounts = new MongoAccountRepositoryAdapter(
                new MongoAccountRepository(mongoClient), new AccountMapper()
            );
            PersonalAccount account = newPersonalAccount(UUID.randomUUID(), "100.00");
            accounts.save(account);

            Account stale = accounts.findById(account.getId()).orElseThrow();
            stale.credit(Money.pln("20.00"));
            assertThrows(IllegalStateException.class, () -> accounts.save(stale));
            assertEquals(Money.pln("100.00"), accounts.findById(account.getId()).orElseThrow().getBalance());

            MongoTransactionExecutor transactions = new MongoTransactionExecutor(mongoClient);
            transactions.execute(repositories -> {
                Account current = repositories.accounts().findById(account.getId()).orElseThrow();
                current.credit(Money.pln("20.00"));
                repositories.accounts().save(current);
                repositories.operations().save(new AtmDeposit(Money.pln("20.00"), account.getId(), "ATM-01"));
                return null;
            });

            assertEquals(Money.pln("120.00"), accounts.findById(account.getId()).orElseThrow().getBalance());
            OperationRepository operations = new MongoOperationRepositoryAdapter(
                new MongoOperationRepository(mongoClient), new OperationMapper()
            );
            assertEquals(1, operations.findByAccountId(account.getId()).size());
        }
    }

    @Test
    void shouldDeleteClientWithoutAccounts() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            ClientRepository clients = new MongoClientRepositoryAdapter(
                new MongoClientRepository(mongoClient), new MongoAccountRepository(mongoClient),
                new ClientMapper(), new AccountMapper()
            );

            Client client = newClient();
            clients.save(client);
            assertTrue(clients.findById(client.getId()).isPresent());

            clients.deleteById(client.getId());
            assertTrue(clients.findById(client.getId()).isEmpty());
        }
    }

    private static Client newClient() {
        return new Client(
            "Anna", "Nowak", new PersonalId(personalId()),
            new Address("Poland", "Lodz", "Testowa", "12", null, "90-001")
        );
    }

    private static PersonalAccount newPersonalAccount(UUID ownerId, String balance) {
        return new PersonalAccount(
            ownerId, new AccountNumber(accountNumber()), Money.pln(balance),
            AccountStatus.ACTIVE, AccountTier.GOLD
        );
    }

    private static String accountNumber() {
        return UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "");
    }

    private static String personalId() {
        return String.format("%011d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L));
    }
}
