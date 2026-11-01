package pl.nbd.infrastructure.adapters;

import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.repository.ClientRepository;
import pl.nbd.infrastructure.documents.client.ClientEmbeddedMgd;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.mappers.ClientMapper;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;
import pl.nbd.infrastructure.repositories.MongoClientRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MongoClientRepositoryAdapter implements ClientRepository {

    private final MongoClientRepository clients;
    private final MongoAccountRepository accounts;
    private final ClientMapper clientMapper;
    private final AccountMapper accountMapper;

    public MongoClientRepositoryAdapter(
        MongoClientRepository clients,
        MongoAccountRepository accounts,
        ClientMapper clientMapper,
        AccountMapper accountMapper
                                       ) {
        this.clients = Objects.requireNonNull(clients);
        this.accounts = Objects.requireNonNull(accounts);
        this.clientMapper = Objects.requireNonNull(clientMapper);
        this.accountMapper = Objects.requireNonNull(accountMapper);
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return clients.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Client> findByPersonalId(PersonalId personalId) {
        Objects.requireNonNull(personalId, "Personal ID must not be null");

        return clients.findByPersonalId(personalId.value()).map(this::toDomain);
    }

    @Override
    public void save(Client client) {
        Objects.requireNonNull(client, "Client must not be null");

        clients.save(clientMapper.toDocument(client));

        for (var account : client.getAccounts()) {
            accounts.save(accountMapper.toDocument(account));
        }
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "Client id must not be null");

        if (!accounts.findByOwnerId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete a client with existing accounts");
        }

        clients.deleteById(id);
    }

    private Client toDomain(ClientEmbeddedMgd document) {
        var clientAccounts = accounts.findByOwnerId(document.getId()).stream().map(accountMapper::toDomain).toList();

        return clientMapper.toDomain(document, clientAccounts);
    }
}