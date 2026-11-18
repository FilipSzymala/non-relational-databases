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

@BsonDiscriminator(value = "interest_capitalization")
public class InterestCapitalizationEmbeddedMgd extends AbstractOperationEmbeddedMgd {
    @BsonProperty("account_id")
    private final UUID accountId;
    private final String month;

    @BsonCreator
    @JsonbCreator
    public InterestCapitalizationEmbeddedMgd(
        @BsonId @JsonbProperty("id") UUID id,
        @BsonProperty("occurred_at") @JsonbProperty("occurredAt") Instant occurredAt,
        @BsonProperty("amount") @JsonbProperty("amount") BigDecimal amount,
        @BsonProperty("account_id") @JsonbProperty("accountId") UUID accountId,
        @BsonProperty("month") @JsonbProperty("month") String month) {
        super(id, occurredAt, amount);
        this.accountId = accountId;
        this.month = month;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getMonth() {
        return month;
    }
}
