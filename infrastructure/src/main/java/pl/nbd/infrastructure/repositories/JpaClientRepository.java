package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.util.Optional;
import java.util.UUID;

public class JpaClientRepository {
    private final EntityManager em;

    public JpaClientRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<ClientEntity> findById(UUID id) {
        return em.createQuery("""
            SELECT DISTINCT c
            FROM ClientEntity c
            LEFT JOIN FETCH c.accounts
            WHERE c.id = :id
            """, ClientEntity.class).setParameter("id", id).getResultStream().findFirst();
    }

    public Optional<ClientEntity> findByPersonalId(String personalId) {
        return em.createQuery("""
            SELECT DISTINCT c
            FROM ClientEntity c
            LEFT JOIN FETCH c.accounts
            WHERE c.personalId.personalId = :personalId
            """, ClientEntity.class).setParameter("personalId", personalId).getResultStream().findFirst();
    }

    public void persist(ClientEntity client) {
        em.persist(client);
    }

    public void deleteById(UUID id) {
        ClientEntity client = em.find(ClientEntity.class, id);
        if (client != null) {
            em.remove(client);
        }
    }
}