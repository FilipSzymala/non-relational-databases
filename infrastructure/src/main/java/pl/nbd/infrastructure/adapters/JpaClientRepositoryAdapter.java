package pl.nbd.infrastructure.adapters;

import jakarta.persistence.EntityManager;
import pl.nbd.bank.bank.account.Account;
import pl.nbd.bank.bank.client.Address;
import pl.nbd.bank.bank.client.Client;
import pl.nbd.bank.bank.client.PersonalId;
import pl.nbd.bank.repositories.ClientRepository;
import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.client.AddressEmbeddable;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.entities.client.PersonalIdEmbeddable;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.mappers.ClientMapper;
import pl.nbd.infrastructure.repositories.JpaClientRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaClientRepositoryAdapter implements ClientRepository {
    private final EntityManager em;
    private final JpaClientRepository repository;
    private final ClientMapper mapper;
    private final AccountMapper accountMapper;

    public JpaClientRepositoryAdapter(EntityManager em) {
        this.em = Objects.requireNonNull(em);
        this.repository = new JpaClientRepository(em);
        this.accountMapper = new AccountMapper();
        this.mapper = new ClientMapper(accountMapper);
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Client> findByPersonalId(PersonalId personalId) {
        Objects.requireNonNull(personalId);
        return repository.findByPersonalId(personalId.value()).map(mapper::toDomain);
    }

    @Override
    public void save(Client client) {
        Objects.requireNonNull(client);

        Optional<ClientEntity> existing = repository.findById(client.getId());
        if (existing.isEmpty()) {
            repository.persist(mapper.toEntity(client));
            return;
        }

        ClientEntity current = existing.get();
        current.setFirstName(client.getFirstName());
        current.setLastName(client.getLastName());
        current.setPersonalId(new PersonalIdEmbeddable(client.getPersonalId().value()));
        Address address = client.getAddress();
        current.setAddress(new AddressEmbeddable(
                address.country(), address.city(), address.street(),
                address.streetNumber(), address.apartmentNumber(), address.zipCode()
        ));

        for (Account account : client.getAccounts()) {
            if (!account.getOwnerId().equals(client.getId())) {
                throw new IllegalArgumentException("Account belongs to another client");
            }

            AccountEntity currentAccount = em.find(AccountEntity.class, account.getId());
            if (currentAccount == null) {
                AccountEntity newAccount = accountMapper.toEntity(account, current);
                current.addAccount(newAccount);
                em.persist(newAccount);
            } else {
                if (!currentAccount.getClient().getId().equals(client.getId())) {
                    throw new IllegalArgumentException("Account belongs to another client");
                }
                AccountEntity updated = accountMapper.toEntity(account, current);
                if (!currentAccount.getClass().equals(updated.getClass())) {
                    throw new IllegalArgumentException("Account type cannot be changed");
                }
                em.merge(updated);
            }
        }
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
