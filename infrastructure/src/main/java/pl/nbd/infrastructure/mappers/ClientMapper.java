package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.infrastructure.documents.client.AddressMgd;
import pl.nbd.infrastructure.documents.client.ClientEmbeddedMgd;
import pl.nbd.infrastructure.documents.client.PersonalIdMgd;

import java.util.List;
import java.util.Objects;

public class ClientMapper {
    public ClientEmbeddedMgd toDocument(Client client) {
        Objects.requireNonNull(client, "Client must not be null");
        PersonalIdMgd personalIdMgd = new PersonalIdMgd(client.getPersonalId().value());
        AddressMgd addressMgd = new AddressMgd(
            client.getAddress().country(),
            client.getAddress().city(),
            client.getAddress().street(),
            client.getAddress().streetNumber(),
            client.getAddress().apartmentNumber(),
            client.getAddress().zipCode()
        );
        return new ClientEmbeddedMgd(
            client.getId(),
            client.getFirstName(),
            client.getLastName(),
            personalIdMgd,
            addressMgd
        );
    }

    public Client toDomain(ClientEmbeddedMgd document, List<Account> accounts) {
        Objects.requireNonNull(document, "Client document must not be null");
        Objects.requireNonNull(accounts, "Accounts must not be null");

        AddressMgd addressMgd = document.getAddress();

        Address address = new Address(
            addressMgd.getCountry(),
            addressMgd.getCity(),
            addressMgd.getStreet(),
            addressMgd.getStreetNumber(),
            addressMgd.getApartmentNumber(),
            addressMgd.getZipCode()
        );

        PersonalId personalId = new PersonalId(document.getPersonalId().getValue());

        return new Client(
            document.getId(),
            document.getFirstName(),
            document.getLastName(),
            personalId,
            address,
            accounts
        );
    }
}
