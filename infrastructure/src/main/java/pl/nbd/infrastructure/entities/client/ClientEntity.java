package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.account.AccountEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class ClientEntity {
    @Id
    private UUID id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Embedded
    private PersonalIdEmbeddable personalId;
    @Embedded
    private AddressEmbeddable address;
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST)
    private List<AccountEntity> accounts = new ArrayList<>();

    protected ClientEntity() {
    }

    public ClientEntity(UUID id, String firstName, String lastName, PersonalIdEmbeddable personalId, AddressEmbeddable address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalId = personalId;
        this.address = address;
    }

    public void addAccount(AccountEntity account) {
        accounts.add(account);
        account.setClient(this);
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public PersonalIdEmbeddable getPersonalId() {
        return personalId;
    }

    public AddressEmbeddable getAddress() {
        return address;
    }

    public List<AccountEntity> getAccounts() {
        return accounts;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPersonalId(PersonalIdEmbeddable personalId) {
        this.personalId = personalId;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
    }
}
