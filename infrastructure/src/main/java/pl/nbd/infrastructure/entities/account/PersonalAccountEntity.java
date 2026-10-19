package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.util.UUID;

@Entity
@DiscriminatorValue("PERSONAL")
public class PersonalAccountEntity extends AccountEntity {
    @Column(name = "tier")
    private String tier;

    protected PersonalAccountEntity() {
        super();
    }

    private PersonalAccountEntity(UUID id, ClientEntity client, String tier) {
        super(id, client);
        this.tier = tier;
    }

    public static PersonalAccountEntity create(UUID id, ClientEntity client, String tier) {
        return new PersonalAccountEntity(id, client, tier);
    }

    public String getTier() {
        return tier;
    }
}