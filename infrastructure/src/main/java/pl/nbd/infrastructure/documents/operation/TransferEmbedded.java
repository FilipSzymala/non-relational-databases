package pl.nbd.infrastructure.documents.operation;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator(value = "transfer")
public class TransferEmbedded extends AbstractOperationEmbedded {
    @BsonProperty("source_account_id")
    private final UUID sourceAccountId;
    @BsonProperty("target_account_id")
    private final UUID targetAccountId;

    @BsonCreator
    public TransferEmbedded(@BsonId UUID id, @BsonProperty("occurred_at") Instant occurredAt, @BsonProperty("amount") BigDecimal amount, @BsonProperty("source_account_id") UUID sourceAccountId, @BsonProperty("target_account_id") UUID targetAccountId) {
        super(id, occurredAt, amount);
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }
}