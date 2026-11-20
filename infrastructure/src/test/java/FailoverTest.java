import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;
import pl.nbd.infrastructure.repositories.MongoClientRepository;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;
import pl.nbd.infrastructure.util.MongoClientFactory;
import pl.nbd.infrastructure.util.MongoDatabaseInitializer;
import pl.nbd.infrastructure.util.MongoTransactionExecutor;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class FailoverTest {

    @Test
    void shouldPreserveDataAndExecuteTransferAfterPrimaryFailure() throws Exception {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoTransactionExecutor transactions = new MongoTransactionExecutor(mongoClient);

            Client client = new Client(
                "Failover", "Test",
                new PersonalId(String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L))),
                new Address("Poland", "Lodz", "Testowa", "67", null, "21-037")
            );

            PersonalAccount source = newAccount(client.getId(), "100.00");
            PersonalAccount target = newAccount(client.getId(), "200.00");
            client.addAccount(source);
            client.addAccount(target);

            transactions.execute(repositories -> {
                repositories.clients().save(client);
                return null;
            });

            transactions.execute(repositories -> {
                assertEquals(2, repositories.clients().findById(client.getId()).orElseThrow().getAccounts().size());
                assertEquals(Money.pln("100.00"), repositories.accounts().findById(source.getId()).orElseThrow().getBalance());
                assertEquals(Money.pln("200.00"), repositories.accounts().findById(target.getId()).orElseThrow().getBalance());
                assertTrue(repositories.operations().findByAccountId(source.getId()).isEmpty());
                return null;
            });

            String previousPrimary = currentPrimary(mongoClient);
            assertNotNull(previousPrimary, "No primary before failure");

            String container = previousPrimary.substring(0, previousPrimary.indexOf(':'));
            assertTrue(container.matches("mongodb[123]"), "Unexpected primary container: " + container);

            boolean stopped = false;

            try {
                docker("stop", container);
                stopped = true;

                String newPrimary = awaitNewPrimary(mongoClient, previousPrimary, Duration.ofSeconds(120));
                assertNotEquals(previousPrimary, newPrimary);
                System.out.println("Previous primary: " + previousPrimary);
                System.out.println("New primary: " + newPrimary);

                Transfer transfer = transactions.execute(repositories -> {
                    assertEquals(2, repositories.clients().findById(client.getId()).orElseThrow().getAccounts().size());

                    Account from = repositories.accounts().findById(source.getId()).orElseThrow();
                    Account to = repositories.accounts().findById(target.getId()).orElseThrow();
                    Money amount = Money.pln("30.00");

                    from.debit(amount);
                    to.credit(amount);
                    repositories.accounts().save(from);
                    repositories.accounts().save(to);

                    Transfer operation = new Transfer(amount, from.getId(), to.getId());
                    repositories.operations().save(operation);
                    return operation;
                });

                transactions.execute(repositories -> {
                    assertEquals(Money.pln("70.00"), repositories.accounts().findById(source.getId()).orElseThrow().getBalance());
                    assertEquals(Money.pln("230.00"), repositories.accounts().findById(target.getId()).orElseThrow().getBalance());
                    assertEquals(1, repositories.operations().findByAccountId(source.getId()).size());
                    assertEquals(1, repositories.operations().findByAccountId(target.getId()).size());
                    assertEquals(transfer.getId(), repositories.operations().findById(transfer.getId()).orElseThrow().getId());
                    return null;
                });

                assertTrue(new MongoClientRepository(mongoClient).findById(client.getId()).isPresent());
                assertTrue(new MongoAccountRepository(mongoClient).findById(source.getId()).isPresent());
                assertTrue(new MongoOperationRepository(mongoClient).findById(transfer.getId()).isPresent());
            } finally {
                if (stopped) {
                    docker("start", container);
                }
            }

            transactions.execute(repositories -> {
                assertEquals(Money.pln("70.00"), repositories.accounts().findById(source.getId()).orElseThrow().getBalance());
                assertEquals(Money.pln("230.00"), repositories.accounts().findById(target.getId()).orElseThrow().getBalance());
                assertEquals(1, repositories.operations().findByAccountId(source.getId()).size());
                return null;
            });
        }
    }

    private static PersonalAccount newAccount(UUID ownerId, String balance) {
        return new PersonalAccount(
            ownerId,
            new AccountNumber(accountNumber()),
            Money.pln(balance),
            AccountStatus.ACTIVE,
            AccountTier.DEFAULT
        );
    }

    private static String accountNumber() {
        return UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "");
    }

    private static String currentPrimary(MongoClient mongoClient) {
        Document hello = mongoClient.getDatabase("admin").runCommand(new Document("hello", 1));
        return hello.getString("primary");
    }

    private static String awaitNewPrimary(
        MongoClient mongoClient,
        String previousPrimary,
        Duration timeout
                                         ) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                Document hello = mongoClient.getDatabase("admin").runCommand(new Document("hello", 1));
                String primary = hello.getString("primary");

                if (primary != null && !primary.equals(previousPrimary)
                    && Boolean.TRUE.equals(hello.getBoolean("isWritablePrimary"))) {
                    return primary;
                }
            } catch (MongoException ignored) {
            }

            Thread.sleep(1000);
        }

        throw new AssertionError("Replica set did not elect a new primary");
    }

    private static void docker(String action, String container) throws Exception {
        Process process = new ProcessBuilder("docker", action, container)
            .inheritIO()
            .start();

        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Docker command timed out: " + action + " " + container);
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException("Docker command failed: " + action + " " + container);
        }
    }
}
