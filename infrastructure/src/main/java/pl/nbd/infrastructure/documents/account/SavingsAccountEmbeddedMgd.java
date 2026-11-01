package pl.nbd.infrastructure.documents.account;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import pl.nbd.infrastructure.documents.account.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@BsonDiscriminator("savings")
public class SavingsAccountEmbeddedMgd extends AbstractAccountEmbeddedMgd {
    @BsonProperty("annual_interest_rate")
    private final BigDecimal annualInterestRate;
    @BsonProperty("last_interest_calculcation_date")
    private final String lastInterestCalculationDate;

    @BsonCreator
    public SavingsAccountEmbeddedMgd(
        @BsonId UUID id,
        @BsonProperty("owner_id") UUID ownerId,
        @BsonProperty("created_at") Instant createdAt,
        @BsonProperty("account_number") String accountNumber,
        @BsonProperty("balance") BigDecimal balance,
        @BsonProperty("status") AccountStatus status,
        @BsonProperty("annual_interest_rate") BigDecimal annualInterestRate,
        @BsonProperty("last_interest_calculcation_date") String lastInterestCalculationDate
                                    ) {
        super(id, ownerId, createdAt, accountNumber, balance, status);
        this.annualInterestRate = annualInterestRate;
        this.lastInterestCalculationDate = lastInterestCalculationDate;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public String getLastInterestCalculationDate() {
        return lastInterestCalculationDate;
    }
}
