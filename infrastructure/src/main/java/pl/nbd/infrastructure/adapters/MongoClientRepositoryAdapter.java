package pl.nbd.infrastructure.adapters;

import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.repository.ClientRepository;

import java.util.Optional;
import java.util.UUID;

public class MongoClientRepositoryAdapter implements ClientRepository {
    Optional<Client> findById(UUID id) {
        return null;
    }

    Optional<Client> findByPersonalId(PersonalId personalId) {
        return null;
    }

    void save(Client client) {

    }

    void deleteById(UUID id) {

    }
}
