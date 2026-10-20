package pl.nbd.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import pl.nbd.infrastructure.entities.operation.OperationEntity;
import pl.nbd.infrastructure.entities.operation.TransferEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaOperationRepository {
    private final EntityManager em;

    public JpaOperationRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<OperationEntity> findById(UUID id) {
        return Optional.ofNullable(em.find(OperationEntity.class, id));
    }

    public List<OperationEntity> findByAccountId(UUID accountId) {
        List<OperationEntity> result = new ArrayList<>(em.createQuery("""
            SELECT o
            FROM OperationEntity o
            WHERE o.account.id = :accountId
            """, OperationEntity.class).setParameter("accountId", accountId).getResultList());

        result.addAll(em.createQuery("""
            SELECT t
            FROM TransferEntity t
            WHERE t.sourceAccount.id = :accountId
               OR t.targetAccount.id = :accountId
            """, TransferEntity.class).setParameter("accountId", accountId).getResultList());

        result.sort(Comparator.comparing(OperationEntity::getOccurredAt).thenComparing(OperationEntity::getId));
        return result;
    }

    public void persist(OperationEntity operation) {
        em.persist(operation);
    }
}
