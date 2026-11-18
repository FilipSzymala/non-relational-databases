package pl.nbd.infrastructure.documents.operation;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator(value = "transfer")
public class TransferEmbeddedMgd extends AbstractOperationEmbeddedMgd {
    @BsonProperty("source_account_id")
    private final UUID sourceAccountId;
    @BsonProperty("target_account_id")
    private final UUID targetAccountId;

    @BsonCreator
    @JsonbCreator
    public TransferEmbeddedMgd(
        @BsonId @JsonbProperty("id") UUID id,
        @BsonProperty("occurred_at") @JsonbProperty("occurredAt") Instant occurredAt,
        @BsonProperty("amount") @JsonbProperty("amount") BigDecimal amount,
        @BsonProperty("source_account_id") @JsonbProperty("sourceAccountId") UUID sourceAccountId,
        @BsonProperty("target_account_id") @JsonbProperty("targetAccountId") UUID targetAccountId) {
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
