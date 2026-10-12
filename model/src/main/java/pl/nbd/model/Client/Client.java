package pl.nbd.model.Client;

import pl.nbd.model.Account.Account;
import pl.nbd.model.Account.enums.AccountStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Client {
    private final UUID id;
    private String firstName;
    private String lastName;
    private PersonalId personalId;
    private Address address;
    private final List<Account> accounts = new ArrayList<>();

    public Client(String firstName, String lastName, PersonalId personalId, Address address) {
        this.id = UUID.randomUUID();
        this.firstName = requireNonBlank(firstName);
        this.lastName = requireNonBlank(lastName);
        this.personalId = Objects.requireNonNull(personalId);
        this.address = Objects.requireNonNull(address);
    }

    public UUID getId() {
        return id;
    }

    public Client(String firstName, String lastName, PersonalId personalId, Address address, List<Account> accounts) {
        this(firstName, lastName, personalId, address);
        Objects.requireNonNull(accounts, "Accounts must not be null").forEach(this::addAccount);
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
        return List.copyOf(accounts);
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name must not be blank");
        }
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name must not be blank");
        }
        this.lastName = lastName;
    }

    public void setPersonalId(PersonalId personalId) {
        this.personalId = Objects.requireNonNull(personalId, "Personal ID must not be null");
    }

    public void setAddress(Address address) {
        this.address = Objects.requireNonNull(address, "Address must not be null");
    }

    public void addAccount(Account account) {
        Objects.requireNonNull(account, "Account must not be null");
        if (!id.equals(account.getOwnerId())) {
            throw new IllegalArgumentException("Account belongs to another client");
        }
        if (accounts.stream().anyMatch(existing -> existing.getId().equals(account.getId()))) {
            throw new IllegalArgumentException("Account is already assigned to this client");
        }
        accounts.add(account);
    }

    public void removeAccount(Account account) {
        Objects.requireNonNull(account, "Account must not be null");
        if (!id.equals(account.getOwnerId())) {
            throw new IllegalArgumentException("Account belongs to another client");
        }
        if (account.getStatus() != AccountStatus.CLOSED) {
            throw new IllegalStateException("Only closed accounts can be removed");
        }
        accounts.removeIf(existing -> existing.getId().equals(account.getId()));
    }

    @Override
    public String toString() {
        return "Client{" + "id=" + id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + ", personalId=" + personalId + ", address=" + address + ", accounts=" + accounts + '}';
    }

    private static String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }
        return value;
    }
}
