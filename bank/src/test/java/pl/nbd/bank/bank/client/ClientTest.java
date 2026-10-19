package pl.nbd.bank.bank.client;

import org.junit.jupiter.api.Test;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.shared.Money;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {
    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Address ADDRESS = new Address("Polska", "Kraków", "Długa", "12", "3", "30-001");
    private static final PersonalId PESEL = new PersonalId("12345678901");

    private static Client newClient() {
        return new Client("Jan", "Kowalski", PESEL, ADDRESS);
    }

    private static Account account(UUID ownerId) {
        return new PersonalAccount(ownerId, new AccountNumber("1111111111111111111111111111111111111111111111111111111111113333"),
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
        Account account = account(client.getId());
        client.addAccount(account);
        assertEquals(1, client.getAccounts().size());
        assertEquals(account.getId(), client.getAccounts().getFirst().getId());
    }

    @Test
    void rejectsAccountOwnedByAnotherClient() {
        Client client = newClient();
        Account foreign = account(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> client.addAccount(foreign));
        assertTrue(client.getAccounts().isEmpty());
    }

    @Test
    void rejectsDuplicateAssignment() {
        Client client = newClient();
        Account account = account(client.getId());
        client.addAccount(account);
        assertThrows(IllegalArgumentException.class, () -> client.addAccount(account));
        assertEquals(1, client.getAccounts().size());
    }

    @Test
    void exposesUnmodifiableAccountList() {
        Client client = newClient();
        assertThrows(UnsupportedOperationException.class, () -> client.getAccounts().add(account(client.getId())));
    }

    @Test
    void removesOnlyClosedAccounts() {
        Client client = newClient();
        Account account = account(client.getId());
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
        Account foreign = account(UUID.randomUUID());
        foreign.close();
        assertThrows(IllegalArgumentException.class, () -> client.removeAccount(foreign));
    }

    @Test
    void reconstructsExistingClientAndAccountsWithoutChangingId() {
        Account a = account(ID);
        Client client = new Client(ID, "Anna", "Nowak", PESEL, ADDRESS, List.of(a));
        assertEquals(ID, client.getId());
        assertEquals(List.of(a), client.getAccounts());
        assertTrue(client.toString().contains("Anna"));
        assertTrue(client.toString().contains(ID.toString()));
        a.close();
        client.removeAccount(a);
        assertTrue(client.getAccounts().isEmpty());
    }

    @Test
    void restoredClientValidatesAllFieldsAndCollections() {
        assertThrows(NullPointerException.class, () -> new Client(null, "A", "B", PESEL, ADDRESS));
        assertThrows(IllegalArgumentException.class, () -> new Client(ID, null, "B", PESEL, ADDRESS));
        assertThrows(IllegalArgumentException.class, () -> new Client(ID, "A", "  ", PESEL, ADDRESS));
        assertThrows(NullPointerException.class, () -> new Client(ID, "A", "B", null, ADDRESS));
        assertThrows(NullPointerException.class, () -> new Client(ID, "A", "B", PESEL, null));
        assertThrows(NullPointerException.class, () -> new Client(ID, "A", "B", PESEL, ADDRESS, null));
        assertThrows(IllegalArgumentException.class, () -> new Client(ID, "A", "B", PESEL, ADDRESS, List.of(account(UUID.randomUUID()))));
    }

    @Test
    void restoredClientRejectsDuplicateAccountAndAllowsEmptyAccounts() {
        Account a = account(ID);
        assertThrows(IllegalArgumentException.class, () -> new Client(ID, "A", "B", PESEL, ADDRESS, List.of(a, a)));
        assertTrue(new Client(ID, "A", "B", PESEL, ADDRESS, List.of()).getAccounts().isEmpty());
    }

    @Test
    void rejectsInvalidNameChangesAndNullPersonalId() {
        Client client = new Client(ID, "A", "B", PESEL, ADDRESS);
        assertThrows(IllegalArgumentException.class, () -> client.setFirstName(null));
        assertThrows(IllegalArgumentException.class, () -> client.setLastName(" "));
        assertThrows(NullPointerException.class, () -> client.setPersonalId(null));
        client.setFirstName("Ewa");
        client.setLastName("Kowalska");
        assertEquals("Ewa", client.getFirstName());
        assertEquals("Kowalska", client.getLastName());
    }

    @Test
    void rejectsNullAccountsAndHandlesRemovalWhenNotFound() {
        Client client = new Client(ID, "A", "B", PESEL, ADDRESS);
        assertThrows(NullPointerException.class, () -> client.addAccount(null));
        assertThrows(NullPointerException.class, () -> client.removeAccount(null));
        Account foreign = account(UUID.randomUUID());
        foreign.close();
        assertThrows(IllegalArgumentException.class, () -> client.removeAccount(foreign));
        Account unassigned = account(ID);
        unassigned.close();
        client.removeAccount(unassigned);
        assertTrue(client.getAccounts().isEmpty());
    }
}
