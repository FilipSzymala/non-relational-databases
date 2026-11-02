import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import pl.nbd.infrastructure.documents.account.AbstractAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.PersonalAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.SavingsAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.enums.AccountStatus;
import pl.nbd.infrastructure.documents.account.enums.AccountTier;
import pl.nbd.infrastructure.documents.client.AddressMgd;
import pl.nbd.infrastructure.documents.client.ClientEmbeddedMgd;
import pl.nbd.infrastructure.documents.client.PersonalIdMgd;
import pl.nbd.infrastructure.documents.operation.AtmDepositEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.AtmWithdrawEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.InterestCapitalizationEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.TransferEmbeddedMgd;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;
import pl.nbd.infrastructure.repositories.MongoClientRepository;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;
import pl.nbd.infrastructure.util.MongoClientFactory;
import pl.nbd.infrastructure.util.MongoDatabaseInitializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MongoRepositoryTest {

    @Test
    void shouldSaveFindAndDeleteClientDocument() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoClientRepository repository = new MongoClientRepository(mongoClient);

            ClientEmbeddedMgd client = new ClientEmbeddedMgd(
                UUID.randomUUID(), "Anna", "Nowak", new PersonalIdMgd(personalId()),
                new AddressMgd("Poland", "Lodz", "Testowa", "12", null, "90-001")
            );

            repository.save(client);

            ClientEmbeddedMgd byId = repository.findById(client.getId()).orElseThrow();
            ClientEmbeddedMgd byPersonalId = repository.findByPersonalId(client.getPersonalId().getValue()).orElseThrow();

            assertEquals(client.getId(), byId.getId());
            assertEquals(client.getId(), byPersonalId.getId());
            assertEquals("Anna", byId.getFirstName());
            assertEquals("Testowa", byId.getAddress().getStreet());
            assertTrue(repository.findAll().stream().anyMatch(document -> document.getId().equals(client.getId())));

            assertTrue(repository.deleteById(client.getId()));
            assertTrue(repository.findById(client.getId()).isEmpty());
        }
    }

    @Test
    void shouldSaveAndFindBothAccountDocumentTypes() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoAccountRepository repository = new MongoAccountRepository(mongoClient);
            UUID ownerId = UUID.randomUUID();
            Instant createdAt = Instant.now();

            PersonalAccountEmbeddedMgd personal = new PersonalAccountEmbeddedMgd(
                UUID.randomUUID(), ownerId, createdAt, accountNumber(), new BigDecimal("100.00"),
                AccountStatus.ACTIVE, AccountTier.GOLD
            );

            SavingsAccountEmbeddedMgd savings = new SavingsAccountEmbeddedMgd(
                UUID.randomUUID(), ownerId, createdAt, accountNumber(), new BigDecimal("500.00"),
                AccountStatus.ACTIVE, new BigDecimal("0.05"), "2026-10"
            );

            repository.save(personal);
            repository.save(savings);

            AbstractAccountEmbeddedMgd restoredPersonal = repository.findById(personal.getId()).orElseThrow();
            AbstractAccountEmbeddedMgd restoredSavings = repository.findByAccountNumber(savings.getAccountNumber()).orElseThrow();

            assertInstanceOf(PersonalAccountEmbeddedMgd.class, restoredPersonal);
            assertInstanceOf(SavingsAccountEmbeddedMgd.class, restoredSavings);
            assertEquals(AccountTier.GOLD, ((PersonalAccountEmbeddedMgd) restoredPersonal).getAccountTier());
            assertEquals("2026-10", ((SavingsAccountEmbeddedMgd) restoredSavings).getLastInterestCalculationDate());
            assertEquals(0, new BigDecimal("500.00").compareTo(restoredSavings.getBalance()));
            assertEquals(2, repository.findByOwnerId(ownerId).size());
            assertTrue(repository.findAll().stream().anyMatch(document -> document.getId().equals(personal.getId())));

            assertTrue(repository.deleteById(personal.getId()));
            assertTrue(repository.deleteById(savings.getId()));
            assertTrue(repository.findByOwnerId(ownerId).isEmpty());
        }
    }

    @Test
    void shouldInsertAndReadFourOperationDocumentTypes() {
        try (MongoClient mongoClient = MongoClientFactory.create("admin", "adminpassword")) {
            new MongoDatabaseInitializer(mongoClient).initialize();
            MongoOperationRepository repository = new MongoOperationRepository(mongoClient);
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            Instant occurredAt = Instant.now();

            AtmDepositEmbeddedMgd deposit = new AtmDepositEmbeddedMgd(
                UUID.randomUUID(), occurredAt, new BigDecimal("100.00"), sourceId, "ATM-01"
            );
            AtmWithdrawEmbeddedMgd withdrawal = new AtmWithdrawEmbeddedMgd(
                UUID.randomUUID(), occurredAt, new BigDecimal("20.00"), sourceId, "ATM-02"
            );
            TransferEmbeddedMgd transfer = new TransferEmbeddedMgd(
                UUID.randomUUID(), occurredAt, new BigDecimal("30.00"), sourceId, targetId
            );
            InterestCapitalizationEmbeddedMgd interest = new InterestCapitalizationEmbeddedMgd(
                UUID.randomUUID(), occurredAt, new BigDecimal("2.00"), sourceId, "2026-10"
            );

            repository.insert(deposit);
            repository.insert(withdrawal);
            repository.insert(transfer);
            repository.insert(interest);

            assertInstanceOf(AtmDepositEmbeddedMgd.class, repository.findById(deposit.getId()).orElseThrow());
            assertInstanceOf(AtmWithdrawEmbeddedMgd.class, repository.findById(withdrawal.getId()).orElseThrow());
            assertInstanceOf(TransferEmbeddedMgd.class, repository.findById(transfer.getId()).orElseThrow());
            assertInstanceOf(InterestCapitalizationEmbeddedMgd.class, repository.findById(interest.getId()).orElseThrow());
            assertEquals(4, repository.findByAccountId(sourceId).size());
            assertEquals(1, repository.findByAccountId(targetId).size());
            assertTrue(repository.findAll().stream().anyMatch(document -> document.getId().equals(transfer.getId())));
            assertThrows(MongoWriteException.class, () -> repository.insert(deposit));
        }
    }

    private static String accountNumber() {
        return UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "");
    }

    private static String personalId() {
        return String.format("%011d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L));
    }
}
