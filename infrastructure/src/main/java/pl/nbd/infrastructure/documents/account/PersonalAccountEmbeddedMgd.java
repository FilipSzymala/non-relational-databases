package pl.nbd.infrastructure.documents.account;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import pl.nbd.infrastructure.documents.account.enums.AccountStatus;
import pl.nbd.infrastructure.documents.account.enums.AccountTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator("personal")
public class PersonalAccountEmbeddedMgd extends AbstractAccountEmbeddedMgd {
    @BsonProperty("account_tier")
    private final AccountTier accountTier;

    @BsonCreator
    public PersonalAccountEmbeddedMgd(
        @BsonId UUID id,
        @BsonProperty("owner_id") UUID ownerId,
        @BsonProperty("created_at") Instant createdAt,
        @BsonProperty("account_number") String accountNumber,
        @BsonProperty("balance") BigDecimal balance,
        @BsonProperty("status") AccountStatus status,
        @BsonProperty("account_tier") AccountTier accountTier
                                     ) {
        super(id, ownerId, createdAt, accountNumber, balance, status);
        this.accountTier = accountTier;
    }

    public AccountTier getAccountTier() {
        return accountTier;
    }
}
