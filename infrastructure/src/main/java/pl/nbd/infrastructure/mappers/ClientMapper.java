package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;

import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.client.AddressEmbeddable;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.entities.client.PersonalIdEmbeddable;

public class ClientMapper {

    private final AccountMapper accountMapper;

    public ClientMapper(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public ClientEntity toEntity(Client client) {
        ClientEntity entity = getEntity(client);

        for (Account account : client.getAccounts()) {
            AccountEntity accountEntity = accountMapper.toEntity(account, entity);

            entity.addAccount(accountEntity);
        }

        return entity;
    }

    private static ClientEntity getEntity(Client client) {
        Address address = client.getAddress();
        PersonalIdEmbeddable personalId = new PersonalIdEmbeddable(client.getPersonalId().value());

        AddressEmbeddable addressEntity = new AddressEmbeddable(address.country(), address.city(), address.street(),
            address.streetNumber(), address.apartmentNumber(), address.zipCode());

        ClientEntity entity = new ClientEntity(client.getId(), client.getFirstName(), client.getLastName(), personalId, addressEntity);
        return entity;
    }

    public Client toDomain(ClientEntity entity) {
        AddressEmbeddable addressEntity = entity.getAddressEmbeddable();
        PersonalId personalId = new PersonalId(entity.getPersonalIdEmbeddable().getPersonalId());

        Address address = new Address(addressEntity.getCountry(), addressEntity.getCity(), addressEntity.getStreet(),
            addressEntity.getStreetNumber(), addressEntity.getApartmentNumber(), addressEntity.getZipCode());

        Client client = new Client(entity.getId(), entity.getFirstName(), entity.getLastName(), personalId, address);

        for (AccountEntity accountEntity : entity.getAccounts()) {
            Account account = accountMapper.toDomain(accountEntity);
            client.addAccount(account);
        }

        return client;
    }
}
