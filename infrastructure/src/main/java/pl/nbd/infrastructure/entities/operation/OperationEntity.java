package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.AccountEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "operations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "operation_type")
@Access(AccessType.FIELD)
public abstract class OperationEntity {
    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    protected OperationEntity() {
    }

    protected OperationEntity(UUID id, Instant occurredAt, BigDecimal amount, AccountEntity account) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.amount = amount;
        this.account = account;
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

    public AccountEntity getAccount() {
        return account;
    }
}