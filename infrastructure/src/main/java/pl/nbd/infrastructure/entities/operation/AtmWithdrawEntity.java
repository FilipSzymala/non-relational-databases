package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.AccountEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@DiscriminatorValue("ATM_WITHDRAW")
public class AtmWithdrawEntity extends OperationEntity {
    @Column(name = "withdraw_atm_id")
    private String atmId;

    protected AtmWithdrawEntity() {
        super();
    }

    private AtmWithdrawEntity(UUID id, Instant occurredAt, BigDecimal amount, AccountEntity account, String atmId) {
        super(id, occurredAt, amount, Objects.requireNonNull(account));
        this.atmId = atmId;
    }

    public static AtmWithdrawEntity create(UUID id, Instant occurredAt, BigDecimal amount, AccountEntity account, String atmId) {
        return new AtmWithdrawEntity(id, occurredAt, amount, account, atmId);
    }

    public String getAtmId() {
        return atmId;
    }
}