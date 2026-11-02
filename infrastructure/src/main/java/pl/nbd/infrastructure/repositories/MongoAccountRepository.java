package pl.nbd.infrastructure.repositories;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import pl.nbd.infrastructure.documents.account.AbstractAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.PersonalAccountEmbeddedMgd;
import pl.nbd.infrastructure.documents.account.SavingsAccountEmbeddedMgd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoAccountRepository extends AbstractMongoRepository {

    private final MongoCollection<BsonDocument> accounts;
    private final ClientSession session;
    private final Map<UUID, BsonDocument> originalDocuments = new HashMap<>();

    public MongoAccountRepository(MongoClient mongoClient) {
        this(mongoClient, null);
    }

    public MongoAccountRepository(MongoClient mongoClient, ClientSession session) {
        super(mongoClient);
        this.accounts = getDatabase().getCollection("accounts", BsonDocument.class);
        this.session = session;
    }

    public void save(AbstractAccountEmbeddedMgd account) {
        Objects.requireNonNull(account, "Account must not be null");

        BsonDocument document = encode(account);
        BsonDocument original = originalDocuments.get(account.getId());

        if (original != null) {
            if (original.equals(document)) {
                return;
            }

            List<Bson> conditions = new ArrayList<>();
            original.forEach((key, value) -> conditions.add(eq(key, value)));

            long matchedCount = accounts.replaceOne(session, and(conditions), document).getMatchedCount();

            if (matchedCount != 1) {
                throw new IllegalStateException("Account changed during transaction");
            }

            originalDocuments.put(account.getId(), document.clone());
            return;
        }

        BsonDocument existing = find(eq("_id", account.getId())).first();

        if (existing != null) {
            if (existing.equals(document)) {
                return;
            }

            throw new IllegalStateException("Existing accounts must be loaded inside a transaction before modification");
        }

        if (session == null) {
            accounts.insertOne(document);
        } else {
            accounts.insertOne(session, document);
            originalDocuments.put(account.getId(), document.clone());
        }
    }

    public Optional<AbstractAccountEmbeddedMgd> findById(UUID id) {
        Objects.requireNonNull(id, "Account id must not be null");

        BsonDocument document = find(eq("_id", id)).first();

        return Optional.ofNullable(document).map(this::decode);
    }

    public Optional<AbstractAccountEmbeddedMgd> findByAccountNumber(String accountNumber) {
        Objects.requireNonNull(accountNumber, "Account number must not be null");

        BsonDocument document = find(eq("account_number", accountNumber)).first();

        return Optional.ofNullable(document).map(this::decode);
    }

    public List<AbstractAccountEmbeddedMgd> findByOwnerId(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner id must not be null");

        return decodeAll(find(eq("owner_id", ownerId)));
    }

    public List<AbstractAccountEmbeddedMgd> findAll() {
        return decodeAll(find(new BsonDocument()));
    }

    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "Account id must not be null");

        if (session == null) {
            return accounts.deleteOne(eq("_id", id)).getDeletedCount() > 0;
        }

        return accounts.deleteOne(session, eq("_id", id)).getDeletedCount() > 0;
    }

    private FindIterable<BsonDocument> find(Bson filter) {
        return session == null ? accounts.find(filter) : accounts.find(session, filter);
    }

    private List<AbstractAccountEmbeddedMgd> decodeAll(FindIterable<BsonDocument> documents) {
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

        AbstractAccountEmbeddedMgd account = switch (type) {
            case "personal" -> decode(document, PersonalAccountEmbeddedMgd.class);
            case "savings" -> decode(document, SavingsAccountEmbeddedMgd.class);
            default -> throw new IllegalStateException("Unsupported account type: " + type);
        };

        if (session != null) {
            originalDocuments.putIfAbsent(account.getId(), document.clone());
        }

        return account;
    }

    private <T extends AbstractAccountEmbeddedMgd> T decode(BsonDocument document, Class<T> type) {
        return accounts.getCodecRegistry().get(type).decode(
            new BsonDocumentReader(document),
            DecoderContext.builder().build()
                                                           );
    }

    private BsonDocument encode(AbstractAccountEmbeddedMgd account) {
        if (account instanceof PersonalAccountEmbeddedMgd personal) {
            return encode(personal, PersonalAccountEmbeddedMgd.class);
        }

        if (account instanceof SavingsAccountEmbeddedMgd savings) {
            return encode(savings, SavingsAccountEmbeddedMgd.class);
        }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getName());
    }

    private <T extends AbstractAccountEmbeddedMgd> BsonDocument encode(T account, Class<T> type) {
        BsonDocument document = new BsonDocument();

        accounts.getCodecRegistry().get(type).encode(
            new BsonDocumentWriter(document),
            account,
            EncoderContext.builder().isEncodingCollectibleDocument(true).build()
                                                    );

        return document;
    }
}
