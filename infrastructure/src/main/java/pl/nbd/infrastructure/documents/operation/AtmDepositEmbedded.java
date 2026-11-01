package pl.nbd.infrastructure.documents.operation;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator(value = "atm_deposit")
public class AtmDepositEmbedded extends AbstractOperationEmbedded {
    @BsonProperty("account_id")
    private final UUID accountId;
    @BsonProperty("atm_id")
    private final String atmId;

    @BsonCreator
    public AtmDepositEmbedded(@BsonId UUID id, @BsonProperty("occurred_at") Instant occurredAt, @BsonProperty("amount") BigDecimal amount, @BsonProperty("account_id") UUID accountId, @BsonProperty("atm_id") String atmId) {
        super(id, occurredAt, amount);
        this.accountId = accountId;
        this.atmId = atmId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAtmId() {
        return atmId;
    }
}