package pl.nbd.infrastructure.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.types.Decimal128;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.SavingsAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.ascending;

public class MongoDatabaseInitializer {

    private final MongoDatabase database;
    private final MongoTransactionExecutor transactions;

    public MongoDatabaseInitializer(MongoClient mongoClient) {
        this.database = mongoClient.getDatabase("bank");
        this.transactions = new MongoTransactionExecutor(mongoClient);
    }

    public void initialize() {
        createCollections();
        createIndexes();
        configureValidation();
        insertInitialData();
    }

    private void createCollections() {
        List<String> collections = database.listCollectionNames().into(new java.util.ArrayList<>());

        for (String name : List.of("clients", "accounts", "operations")) {
            if (!collections.contains(name)) {
                database.createCollection(name);
            }
        }
    }

    private void createIndexes() {
        database.getCollection("clients").createIndex(
            ascending("personal_id.value"),
            new IndexOptions().unique(true)
        );

        database.getCollection("accounts").createIndex(
            ascending("account_number"),
            new IndexOptions().unique(true)
        );

        database.getCollection("accounts").createIndex(ascending("owner_id"));
        database.getCollection("operations").createIndex(ascending("account_id"));
        database.getCollection("operations").createIndex(ascending("source_account_id"));
        database.getCollection("operations").createIndex(ascending("target_account_id"));

        database.getCollection("operations").createIndex(
            ascending("account_id", "month"),
            new IndexOptions()
                .unique(true)
                .partialFilterExpression(eq("_t", "interest_capitalization"))
        );
    }

    private void configureValidation() {
        Document schema = new Document("bsonType", "object")
            .append("required", List.of(
                "_id", "_t", "owner_id", "created_at", "account_number", "balance", "status"
            ))
            .append("properties", new Document()
                .append("_t", new Document("enum", List.of("personal", "savings")))
                .append("balance", new Document("bsonType", "decimal"))
                .append("account_number", new Document("bsonType", "string")
                    .append("minLength", 64)
                    .append("maxLength", 64))
                .append("status", new Document("enum", List.of("ACTIVE", "BLOCKED", "CLOSED")))
            );

        Document allowedBalance = new Document("$or", List.of(
            accountBalance("savings", null, "0.00"),
            accountBalance("personal", "DEFAULT", "0.00"),
            accountBalance("personal", "SILVER", "-500.00"),
            accountBalance("personal", "GOLD", "-2000.00"),
            accountBalance("personal", "DIAMOND", "-5000.00")
        ));

        Document closedAccount = new Document("$or", List.of(
            new Document("status", new Document("$ne", "CLOSED")),
            new Document("balance", decimal("0.00"))
        ));

        database.runCommand(new Document("collMod", "accounts")
            .append("validator", new Document("$and", List.of(
                new Document("$jsonSchema", schema),
                allowedBalance,
                closedAccount
            )))
            .append("validationLevel", "strict")
            .append("validationAction", "error")
        );
    }

    private Document accountBalance(String type, String tier, String minimum) {
        Document condition = new Document("_t", type)
            .append("balance", new Document("$gte", decimal(minimum)));

        if (tier != null) {
            condition.append("account_tier", tier);
        }

        return condition;
    }

    private Decimal128 decimal(String value) {
        return new Decimal128(new BigDecimal(value));
    }

    private void insertInitialData() {
        transactions.execute(repositories -> {
            PersonalId personalId = new PersonalId("12345678901");

            if (repositories.clients().findByPersonalId(personalId).isPresent()) {
                return null;
            }

            Client client = new Client(
                "Jan",
                "Kowalski",
                personalId,
                new Address("Poland", "Lodz", "Piotrkowska", "10", "5", "90-001")
            );

            client.addAccount(new PersonalAccount(
                client.getId(),
                new AccountNumber("1000000000000000000000000000000000000000000000000000000000000001"),
                Money.pln("1000.00"),
                AccountStatus.ACTIVE,
                AccountTier.DEFAULT
            ));

            client.addAccount(new SavingsAccount(
                client.getId(),
                new AccountNumber("1000000000000000000000000000000000000000000000000000000000000002"),
                Money.pln("5000.00"),
                AccountStatus.ACTIVE,
                new BigDecimal("0.05"),
                null
            ));

            repositories.clients().save(client);
            return null;
        });
    }

    public static void main(String[] args) {
        String username = System.getenv().getOrDefault("MONGO_USERNAME", "admin");
        String password = System.getenv().getOrDefault("MONGO_PASSWORD", "adminpassword");

        try (MongoClient mongoClient = MongoClientFactory.create(username, password)) {
            new MongoDatabaseInitializer(mongoClient).initialize();
        }
    }
}
