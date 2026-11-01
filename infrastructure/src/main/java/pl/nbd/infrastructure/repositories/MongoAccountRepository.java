package pl.nbd.infrastructure.repositories;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import pl.nbd.infrastructure.documents.account.AbstractAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.PersonalAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.SavingsAccountEmbeddedMgd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class MongoAccountRepository extends AbstractMongoRepository {

    private final MongoCollection<BsonDocument> accounts;
    private final MongoCollection<PersonalAccountEmbeddedMgd> personalAccounts;
    private final MongoCollection<SavingsAccountEmbeddedMgd> savingsAccounts;

    public MongoAccountRepository(MongoClient mongoClient) {
        super(mongoClient);

        var database = getDatabase();

        this.accounts = database.getCollection("accounts", BsonDocument.class);

        this.personalAccounts = database.getCollection("accounts", PersonalAccountEmbeddedMgd.class);

        this.savingsAccounts = database.getCollection("accounts", SavingsAccountEmbeddedMgd.class);
    }

    public void save(AbstractAccountEmbeddedMgd account) {
        Objects.requireNonNull(account, "Account must not be null");

        ReplaceOptions options = new ReplaceOptions().upsert(true);

        if (account instanceof PersonalAccountEmbeddedMgd personal) {
            personalAccounts.replaceOne(eq("_id", personal.getId()), personal, options);
            return;
        }

        if (account instanceof SavingsAccountEmbeddedMgd savings) {
            savingsAccounts.replaceOne(eq("_id", savings.getId()), savings, options);
            return;
        }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getName());
    }

    public Optional<AbstractAccountEmbeddedMgd> findById(UUID id) {
        Objects.requireNonNull(id, "Account id must not be null");

        BsonDocument document = accounts.find(eq("_id", id)).first();

        return Optional.ofNullable(document).map(this::decode);
    }

    public Optional<AbstractAccountEmbeddedMgd> findByAccountNumber(String accountNumber) {
        Objects.requireNonNull(accountNumber, "Account number must not be null");

        BsonDocument document = accounts.find(eq("account_number", accountNumber)).first();

        return Optional.ofNullable(document).map(this::decode);
    }

    public List<AbstractAccountEmbeddedMgd> findByOwnerId(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner id must not be null");

        return decodeAll(accounts.find(eq("owner_id", ownerId)));
    }

    public List<AbstractAccountEmbeddedMgd> findAll() {
        return decodeAll(accounts.find());
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "Account id must not be null");

        return accounts.deleteOne(eq("_id", id)).getDeletedCount() > 0;
    }

    private List<AbstractAccountEmbeddedMgd> decodeAll(
        FindIterable<BsonDocument> documents
                                                      ) {
        List<AbstractAccountEmbeddedMgd> result = new ArrayList<>();

        for (BsonDocument document : documents) {
            result.add(decode(document));
        }

        return result;
    }

    private AbstractAccountEmbeddedMgd decode(BsonDocument document) {
        if (!document.isString("_t")) {
            throw new IllegalStateException("Missing or invalid account discriminator");
        }

        String type = document.getString("_t").getValue();

        return switch (type) {
            case "personal" -> decode(document, PersonalAccountEmbeddedMgd.class);
            case "savings" -> decode(document, SavingsAccountEmbeddedMgd.class);
            default -> throw new IllegalStateException("Unsupported account type: " + type);
        };
    }

    private <T extends AbstractAccountEmbeddedMgd> T decode(BsonDocument document, Class<T> type) {
        return accounts.getCodecRegistry().get(type).decode(
            new BsonDocumentReader(document),
            DecoderContext.builder().build());
    }
}