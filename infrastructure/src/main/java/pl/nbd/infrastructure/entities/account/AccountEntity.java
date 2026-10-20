package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type")
public abstract class AccountEntity {
    @Id
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Embedded
    private AccountNumberEmbeddable accountNumber;

    @Column(
        name = "balance",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    protected AccountEntity() {
    }

    protected AccountEntity(UUID id, ClientEntity client, Instant createdAt,
                            AccountNumberEmbeddable accountNumber, BigDecimal balance,
                            AccountStatus status) {
        this.id = id;
        this.client = client;
        this.createdAt = createdAt;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AccountNumberEmbeddable getAccountNumberEmbeddable() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
