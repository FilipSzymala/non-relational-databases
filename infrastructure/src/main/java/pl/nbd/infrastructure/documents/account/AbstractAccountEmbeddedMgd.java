package pl.nbd.infrastructure.documents.account;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import pl.nbd.infrastructure.documents.account.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator
public abstract class AbstractAccountEmbeddedMgd {
    @BsonId
    private final UUID id;
    @BsonProperty("owner_id")
    private final UUID ownerId;
    @BsonProperty("created_at")
    private final Instant createdAt;
    @BsonProperty("account_number")
    private final String accountNumber;
    private final BigDecimal balance;
    private final AccountStatus status;

    protected AbstractAccountEmbeddedMgd(UUID id, UUID ownerId, Instant createdAt, String accountNumber, BigDecimal balance, AccountStatus status) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
