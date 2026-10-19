package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.AccountEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@DiscriminatorValue("TRANSFER")
public class TransferEntity extends OperationEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private AccountEntity sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private AccountEntity targetAccount;

    protected TransferEntity() {
        super();
    }

    private TransferEntity(UUID id, Instant occurredAt, BigDecimal amount, AccountEntity sourceAccount, AccountEntity targetAccount) {
        super(id, occurredAt, amount, null);
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
    }

    public static TransferEntity create(UUID id, Instant occurredAt, BigDecimal amount, AccountEntity sourceAccount, AccountEntity targetAccount) {
        return new TransferEntity(id, occurredAt, amount, sourceAccount, targetAccount);
    }

    public AccountEntity getSourceAccount() {
        return sourceAccount;
    }

    public AccountEntity getTargetAccount() {
        return targetAccount;
    }
}