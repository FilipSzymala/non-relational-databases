package pl.nbd.bank.bank.client;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {
    private static final Address ADDRESS = new Address("Polska", "Kraków", "Długa", "12", "3", "30-001");
    private static final PersonalId PESEL = new PersonalId("12345678901");

    private static Client newClient() {
        return new Client("Jan", "Kowalski", PESEL, ADDRESS);
    }

    private static Account newAccount(UUID ownerId) {
        return new PersonalAccount(ownerId, new AccountNumber(UUID.randomUUID().toString()),
                Money.ZERO, AccountStatus.ACTIVE, AccountTier.DEFAULT);
    }

    @Test
    void createsClientWithIdAndProvidedDetails() {
        Client client = newClient();
        assertNotNull(client.getId());
        assertEquals("Jan", client.getFirstName());
        assertEquals("Kowalski", client.getLastName());
        assertEquals(PESEL, client.getPersonalId());
        assertEquals(ADDRESS, client.getAddress());
        assertTrue(client.getAccounts().isEmpty());
    }

    @Test
    void changesClientDetails() {
        Client client = newClient();
        PersonalId newPesel = new PersonalId("98765432109");
        Address newAddress = new Address("Polska", "Gdańsk", "Morska", "4", null, "80-001");
        client.setFirstName("Anna");
        client.setLastName("Nowak");
        client.setPersonalId(newPesel);
        client.setAddress(newAddress);
        assertEquals("Anna", client.getFirstName());
        assertEquals("Nowak", client.getLastName());
        assertEquals(newPesel, client.getPersonalId());
        assertEquals(newAddress, client.getAddress());
    }

    @Test
    void rejectsBlankNamesAndMissingDetails() {
        assertThrows(IllegalArgumentException.class, () -> new Client(" ", "Kowalski", PESEL, ADDRESS));
        assertThrows(IllegalArgumentException.class, () -> new Client("Jan", null, PESEL, ADDRESS));
        assertThrows(NullPointerException.class, () -> new Client("Jan", "Kowalski", null, ADDRESS));
        Client client = newClient();
        assertThrows(IllegalArgumentException.class, () -> client.setFirstName(""));
        assertThrows(IllegalArgumentException.class, () -> client.setLastName(null));
        assertThrows(NullPointerException.class, () -> client.setAddress(null));
    }

    @Test
    void assignsAccountOwnedByClient() {
        Client client = newClient();
        Account account = newAccount(client.getId());
        client.addAccount(account);
        assertEquals(1, client.getAccounts().size());
        assertEquals(account.getId(), client.getAccounts().getFirst().getId());
    }

    @Test
    void rejectsAccountOwnedByAnotherClient() {
        Client client = newClient();
        Account foreign = newAccount(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> client.addAccount(foreign));
        assertTrue(client.getAccounts().isEmpty());
    }

    @Test
    void rejectsDuplicateAssignment() {
        Client client = newClient();
        Account account = newAccount(client.getId());
        client.addAccount(account);
        assertThrows(IllegalArgumentException.class, () -> client.addAccount(account));
        assertEquals(1, client.getAccounts().size());
    }

    @Test
    void exposesUnmodifiableAccountList() {
        Client client = newClient();
        assertThrows(UnsupportedOperationException.class, () -> client.getAccounts().add(newAccount(client.getId())));
    }

    @Test
    void removesOnlyClosedAccounts() {
        Client client = newClient();
        Account account = newAccount(client.getId());
        client.addAccount(account);
        assertThrows(IllegalStateException.class, () -> client.removeAccount(account));
        assertEquals(1, client.getAccounts().size());
        account.close();
        client.removeAccount(account);
        assertTrue(client.getAccounts().isEmpty());
    }

    @Test
    void rejectsRemovingAccountOwnedByAnotherClient() {
        Client client = newClient();
        Account foreign = newAccount(UUID.randomUUID());
        foreign.close();
        assertThrows(IllegalArgumentException.class, () -> client.removeAccount(foreign));
    }
}
