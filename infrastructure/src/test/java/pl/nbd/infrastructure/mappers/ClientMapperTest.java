package pl.nbd.infrastructure.mappers;

import org.junit.jupiter.api.Test;

import pl.nbd.bank.bank.account.AccountNumber;
import pl.nbd.bank.bank.account.PersonalAccount;
import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.account.enums.AccountTier;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.bank.shared.Money;

import pl.nbd.infrastructure.entities.account.PersonalAccountEntity;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {
    private final ClientMapper mapper = new ClientMapper(new AccountMapper());

    @Test
    void shouldMapClientWithoutAccounts() {
        UUID clientId = UUID.randomUUID();

        Client client = new Client(
            clientId, "Kamil", "Kowalenko", new PersonalId("12345678901"),
            new Address("Polska", "Płock", "Komendancka", "60", "12", "12-321")
        );

        ClientEntity entity = mapper.toEntity(client);

        assertEquals(clientId, entity.getId());
        assertEquals("Kamil", entity.getFirstName());
        assertEquals("Kowalenko", entity.getLastName());
        assertEquals("12345678901", entity.getPersonalIdEmbeddable().getPersonalId());
        assertEquals("Płock", entity.getAddressEmbeddable().getCity());
        assertTrue(entity.getAccounts().isEmpty());

        Client restored = mapper.toDomain(entity);

        assertEquals(client.getId(), restored.getId());
        assertEquals(client.getFirstName(), restored.getFirstName());
        assertEquals(client.getLastName(), restored.getLastName());
        assertEquals(client.getPersonalId(), restored.getPersonalId());
        assertEquals(client.getAddress(), restored.getAddress());
        assertTrue(restored.getAccounts().isEmpty());
    }

    @Test
    void shouldMapClientWithPersonalAccount() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        Client client = new Client(
            clientId, "Kamil", "Kowalenko",
            new PersonalId("12345678901"),
            new Address("Polska", "Płock", "Komendancka", "60", "12", "12-321")
        );

        PersonalAccount account = new PersonalAccount(
            accountId, clientId, Instant.parse("2026-10-19T10:00:00Z"),
            new AccountNumber("0000000000111111111122222222223333333333444444444455555555556666"),
            Money.pln("1250.50"), AccountStatus.ACTIVE, AccountTier.GOLD
        );

        client.addAccount(account);

        ClientEntity entity = mapper.toEntity(client);

        assertEquals(1, entity.getAccounts().size());

        PersonalAccountEntity accountEntity = assertInstanceOf(PersonalAccountEntity.class, entity.getAccounts().getFirst());

        assertEquals(accountId, accountEntity.getId());

        assertSame(entity, accountEntity.getClient());

        Client restored = mapper.toDomain(entity);

        assertEquals(clientId, restored.getId());
        assertEquals(1, restored.getAccounts().size());

        PersonalAccount restoredAccount = assertInstanceOf(PersonalAccount.class, restored.getAccounts().getFirst());

        assertEquals(accountId, restoredAccount.getId());
        assertEquals(clientId, restoredAccount.getOwnerId());
        assertEquals(account.getAccountNumber(), restoredAccount.getAccountNumber());
        assertEquals(account.getBalance(), restoredAccount.getBalance());
        assertEquals(account.getTier(), restoredAccount.getTier());
    }
}
