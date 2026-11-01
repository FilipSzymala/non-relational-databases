package pl.nbd.infrastructure.repositories;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import pl.nbd.infrastructure.documents.operation.AbstractOperationEmbedded;
import pl.nbd.infrastructure.documents.operation.AtmDepositEmbedded;
import pl.nbd.infrastructure.documents.operation.AtmWithdrawEmbedded;
import pl.nbd.infrastructure.documents.operation.InterestCapitalizationEmbedded;
import pl.nbd.infrastructure.documents.operation.TransferEmbedded;

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
    private final MongoCollection<AtmDepositEmbedded> deposits;
    private final MongoCollection<AtmWithdrawEmbedded> withdrawals;
    private final MongoCollection<InterestCapitalizationEmbedded> capitalizations;
    private final MongoCollection<TransferEmbedded> transfers;

    public MongoOperationRepository(MongoClient mongoClient) {
        super(mongoClient);

        var database = mongoClient.getDatabase("bank");
        this.operations = database.getCollection("operations", BsonDocument.class);
        this.deposits = database.getCollection("operations", AtmDepositEmbedded.class);
        this.withdrawals = database.getCollection("operations", AtmWithdrawEmbedded.class);
        this.capitalizations = database.getCollection("operations", InterestCapitalizationEmbedded.class);
        this.transfers = database.getCollection("operations", TransferEmbedded.class);
    }

    public void insert(AbstractOperationEmbedded operation) {
        Objects.requireNonNull(operation, "Operation must not be null");

        if (operation instanceof AtmDepositEmbedded deposit) {
            deposits.insertOne(deposit);
            return;
        }

        if (operation instanceof AtmWithdrawEmbedded withdraw) {
            withdrawals.insertOne(withdraw);
            return;
        }

        if (operation instanceof InterestCapitalizationEmbedded capitalization) {
            capitalizations.insertOne(capitalization);
            return;
        }

        if (operation instanceof TransferEmbedded transfer) {
            transfers.insertOne(transfer);
            return;
        }

        throw new IllegalArgumentException("Unsupported operation type: " + operation.getClass().getName());
    }

    public Optional<AbstractOperationEmbedded> findById(UUID id) {
        Objects.requireNonNull(id, "Operation id must not be null");

        BsonDocument document = operations.find(eq("_id", id)).first();

        return Optional.ofNullable(document).map(this::decode);
    }

    public List<AbstractOperationEmbedded> findByAccountId(UUID accountId) {
        Objects.requireNonNull(accountId, "Account id must not be null");

        return decodeAll(operations.find(or(
            eq("account_id", accountId),
            eq("source_account_id", accountId),
            eq("target_account_id", accountId)
                                           )).sort(ascending("occurred_at", "_id")));
    }

    public List<AbstractOperationEmbedded> findAll() {
        return decodeAll(operations.find().sort(ascending("occurred_at", "_id")));
    }

    private List<AbstractOperationEmbedded> decodeAll(
        FindIterable<BsonDocument> documents
                                                     ) {
        List<AbstractOperationEmbedded> result = new ArrayList<>();

        for (BsonDocument document : documents) {
            result.add(decode(document));
        }

        return result;
    }

    private AbstractOperationEmbedded decode(BsonDocument document) {
        if (!document.isString("_t")) {
            throw new IllegalStateException("Missing or invalid operation discriminator");
        }

        String type = document.getString("_t").getValue();

        return switch (type) {
            case "atm_deposit" -> decode(document, AtmDepositEmbedded.class);
            case "atm_withdraw" -> decode(document, AtmWithdrawEmbedded.class);
            case "interest_capitalization" -> decode(document, InterestCapitalizationEmbedded.class);
            case "transfer" -> decode(document, TransferEmbedded.class);
            default -> throw new IllegalStateException("Unsupported operation type: " + type);
        };
    }

    private <T extends AbstractOperationEmbedded> T decode(BsonDocument document, Class<T> type) {
        return operations.getCodecRegistry().get(type).decode(
            new BsonDocumentReader(document),
            DecoderContext.builder().build());
    }
}
