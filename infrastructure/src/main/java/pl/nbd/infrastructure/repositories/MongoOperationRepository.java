package pl.nbd.infrastructure.repositories;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import org.bson.conversions.Bson;
import pl.nbd.infrastructure.documents.operation.AbstractOperationEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.AtmDepositEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.AtmWithdrawEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.InterestCapitalizationEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.TransferEmbeddedMgd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Sorts.ascending;

public class MongoOperationRepository extends AbstractMongoRepository {

    private final MongoCollection<BsonDocument> operations;
    private final MongoCollection<AtmDepositEmbeddedMgd> deposits;
    private final MongoCollection<AtmWithdrawEmbeddedMgd> withdrawals;
    private final MongoCollection<InterestCapitalizationEmbeddedMgd> capitalizations;
    private final MongoCollection<TransferEmbeddedMgd> transfers;
    private final ClientSession session;

    public MongoOperationRepository(MongoClient mongoClient) {
        this(mongoClient, null);
    }

    public MongoOperationRepository(MongoClient mongoClient, ClientSession session) {
        super(mongoClient);
        this.session = session;

        var database = getDatabase();
        this.operations = database.getCollection("operations", BsonDocument.class);
        this.deposits = database.getCollection("operations", AtmDepositEmbeddedMgd.class);
        this.withdrawals = database.getCollection("operations", AtmWithdrawEmbeddedMgd.class);
        this.capitalizations = database.getCollection("operations", InterestCapitalizationEmbeddedMgd.class);
        this.transfers = database.getCollection("operations", TransferEmbeddedMgd.class);
    }

    public void insert(AbstractOperationEmbeddedMgd operation) {
        Objects.requireNonNull(operation, "Operation must not be null");

        if (operation instanceof AtmDepositEmbeddedMgd deposit) {
            insert(deposits, deposit);
            return;
        }

        if (operation instanceof AtmWithdrawEmbeddedMgd withdraw) {
            insert(withdrawals, withdraw);
            return;
        }

        if (operation instanceof InterestCapitalizationEmbeddedMgd capitalization) {
            insert(capitalizations, capitalization);
            return;
        }

        if (operation instanceof TransferEmbeddedMgd transfer) {
            insert(transfers, transfer);
            return;
        }

        throw new IllegalArgumentException("Unsupported operation type: " + operation.getClass().getName());
    }

    private <T extends AbstractOperationEmbeddedMgd> void insert(MongoCollection<T> collection, T operation) {
        if (session == null) {
            collection.insertOne(operation);
        } else {
            collection.insertOne(session, operation);
        }
    }

    public Optional<AbstractOperationEmbeddedMgd> findById(UUID id) {
        Objects.requireNonNull(id, "Operation id must not be null");

        return Optional.ofNullable(find(eq("_id", id)).first()).map(this::decode);
    }

    public List<AbstractOperationEmbeddedMgd> findByAccountId(UUID accountId) {
        Objects.requireNonNull(accountId, "Account id must not be null");

        return decodeAll(find(or(
            eq("account_id", accountId),
            eq("source_account_id", accountId),
            eq("target_account_id", accountId)
        )).sort(ascending("occurred_at", "_id")));
    }

    public List<AbstractOperationEmbeddedMgd> findAll() {
        return decodeAll(find(new BsonDocument()).sort(ascending("occurred_at", "_id")));
    }

    private FindIterable<BsonDocument> find(Bson filter) {
        return session == null ? operations.find(filter) : operations.find(session, filter);
    }

    private List<AbstractOperationEmbeddedMgd> decodeAll(FindIterable<BsonDocument> documents) {
        List<AbstractOperationEmbeddedMgd> result = new ArrayList<>();

        for (BsonDocument document : documents) {
            result.add(decode(document));
        }

        return result;
    }

    private AbstractOperationEmbeddedMgd decode(BsonDocument document) {
        if (!document.isString("_t")) {
            throw new IllegalStateException("Missing or invalid operation discriminator");
        }

        String type = document.getString("_t").getValue();

        return switch (type) {
            case "atm_deposit" -> decode(document, AtmDepositEmbeddedMgd.class);
            case "atm_withdraw" -> decode(document, AtmWithdrawEmbeddedMgd.class);
            case "interest_capitalization" -> decode(document, InterestCapitalizationEmbeddedMgd.class);
            case "transfer" -> decode(document, TransferEmbeddedMgd.class);
            default -> throw new IllegalStateException("Unsupported operation type: " + type);
        };
    }

    private <T extends AbstractOperationEmbeddedMgd> T decode(BsonDocument document, Class<T> type) {
        return operations.getCodecRegistry().get(type).decode(
            new BsonDocumentReader(document),
            DecoderContext.builder().build());
    }
}
