package pl.nbd.bank.repository;

import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Optional<Client> findById(UUID id);
    Optional<Client> findByPersonalId(PersonalId personalId);
    void save(Client client);
    void deleteById(UUID id);
}
