package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;
import pl.nbd.infrastructure.entities.account.enums.AccountStatus;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@DiscriminatorValue("PERSONAL")
public class PersonalAccountEntity extends AccountEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private AccountTier tier;

    protected PersonalAccountEntity() {
        super();
    }

    private PersonalAccountEntity(UUID id, ClientEntity client, Instant createdAt,
                                  AccountNumberEmbeddable accountNumber, BigDecimal balance,
                                  AccountStatus status, AccountTier tier) {
        super(id, client, createdAt, accountNumber, balance, status);
        this.tier = tier;
    }

    public static PersonalAccountEntity create(UUID id, ClientEntity client, Instant createdAt,
                                               AccountNumberEmbeddable accountNumber, BigDecimal balance,
                                               AccountStatus status, AccountTier tier) {
        return new PersonalAccountEntity(id, client, createdAt, accountNumber, balance, status, tier);
    }

    public AccountTier getTier() {
        return tier;
    }

    public void setTier(AccountTier tier) {
        this.tier = tier;
    }
}