package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.enums.AccountTier;
import pl.nbd.infrastructure.entities.client.ClientEntity;

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

    private PersonalAccountEntity(UUID id, ClientEntity client, AccountTier tier) {
        super(id, client);
        this.tier = tier;
    }

    public static PersonalAccountEntity create(UUID id, ClientEntity client, AccountTier tier) {
        return new PersonalAccountEntity(id, client, tier);
    }

    public AccountTier getTier() {
        return tier;
    }
}