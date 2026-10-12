package pl.nbd.model.repository;

import pl.nbd.model.Client.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Optional<Client> findById(UUID id);
    Optional<Client> findByPersonalId(String personalId);
    void save(Client client);
    void deleteById(UUID id);
}
