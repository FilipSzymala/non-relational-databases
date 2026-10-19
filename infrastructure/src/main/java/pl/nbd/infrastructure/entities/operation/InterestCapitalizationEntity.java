package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@DiscriminatorValue("INTEREST_CAPITALIZATION")
public class InterestCapitalizationEntity extends OperationEntity {
    @Column(name = "capitalization_month", length = 7)
    private YearMonth month;

    protected InterestCapitalizationEntity() {
        super();
    }

    private InterestCapitalizationEntity(UUID id, Instant occurredAt, BigDecimal amount, SavingsAccountEntity account, YearMonth month) {
        super(id, occurredAt, amount, account);
        this.month = month;
    }

    public static InterestCapitalizationEntity create(UUID id, Instant occurredAt, BigDecimal amount, SavingsAccountEntity account, YearMonth month) {
        return new InterestCapitalizationEntity(id, occurredAt, amount, account, month);
    }

    public YearMonth getMonth() {
        return month;
    }
}