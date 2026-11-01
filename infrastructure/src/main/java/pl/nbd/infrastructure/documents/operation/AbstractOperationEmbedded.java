package pl.nbd.infrastructure.documents.operation;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator
public abstract class AbstractOperationEmbedded {
    @BsonId
    private final UUID id;
    @BsonProperty("occurred_at")
    private final Instant occurredAt;
    private final BigDecimal amount;

    protected AbstractOperationEmbedded(UUID id, Instant occurredAt, BigDecimal amount) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}