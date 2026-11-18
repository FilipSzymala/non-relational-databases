package pl.nbd.infrastructure.documents.operation;

import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator
@JsonbTypeInfo(key = "_t", value = {
    @JsonbSubtype(alias = "atm_deposit", type = AtmDepositEmbeddedMgd.class),
    @JsonbSubtype(alias = "atm_withdraw", type = AtmWithdrawEmbeddedMgd.class),
    @JsonbSubtype(alias = "interest_capitalization", type = InterestCapitalizationEmbeddedMgd.class),
    @JsonbSubtype(alias = "transfer", type = TransferEmbeddedMgd.class)
})
public abstract class AbstractOperationEmbeddedMgd {
    @BsonId
    private final UUID id;
    @BsonProperty("occurred_at")
    private final Instant occurredAt;
    private final BigDecimal amount;

    protected AbstractOperationEmbeddedMgd(UUID id, Instant occurredAt, BigDecimal amount) {
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
