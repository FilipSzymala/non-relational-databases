package pl.nbd.model.Client;

import pl.nbd.model.Account.Account;

import java.util.List;
import java.util.UUID;

public class Client {
    private final UUID id;
    private String firstName;
    private String lastName;
    private PersonalId personalId;
    private Address address;
    private List<Account> accounts;

    public Client(String firstName, String lastName, PersonalId personalId, Address address, List<Account> accounts) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalId = personalId;
        this.address = address;
        this.accounts = accounts;
    }

    public Client(String firstName, String lastName, PersonalId personalId, Address address) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalId = personalId;
        this.address = address;
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

    public PersonalId getPersonalId() {
        return personalId;
    }

    public Address getAddress() {
        return address;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPersonalId(PersonalId personalId) {
        this.personalId = personalId;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
    }

    @Override
    public String toString() {
        return "Client{" +
            "id=" + id +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", personalId=" + personalId +
            ", address=" + address +
            ", accounts=" + accounts +
            '}';
    }
}
