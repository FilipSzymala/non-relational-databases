package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;

import pl.nbd.infrastructure.entities.account.AccountEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaAccountRepository {
    private final EntityManager em;

    public JpaAccountRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<AccountEntity> findById(UUID id) {
        return Optional.ofNullable(
            em.find(AccountEntity.class, id)
        );
    }

    public Optional<AccountEntity> findByAccountNumber(String number) {
        return em.createQuery("""
                SELECT a
                FROM AccountEntity a
                WHERE a.accountNumber.accountNumber = :number
                """, AccountEntity.class)
            .setParameter("number", number)
            .getResultStream()
            .findFirst();
    }

    public List<AccountEntity> findByOwnerId(UUID ownerId) {
        return em.createQuery("""
                SELECT a
                FROM AccountEntity a
                WHERE a.client.id = :ownerId
                """, AccountEntity.class)
            .setParameter("ownerId", ownerId)
            .getResultList();
    }

    public void persist(AccountEntity account) {
        em.persist(account);
    }

    public void deleteById(UUID id) {
        AccountEntity account = em.find(AccountEntity.class, id);

        if (account != null) {
            em.remove(account);
        }
    }
}
