package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.operation.*;
import pl.nbd.bank.bank.shared.Money;

import pl.nbd.infrastructure.entities.account.AccountEntity;
import pl.nbd.infrastructure.entities.account.SavingsAccountEntity;
import pl.nbd.infrastructure.entities.operation.*;

import java.util.Map;
import java.util.UUID;

public class OperationMapper {
    public OperationEntity toEntity(Operation operation, Map<UUID, AccountEntity> accounts) {
        if (operation instanceof AtmDeposit deposit) {
            return AtmDepositEntity.create(
                deposit.getId(), deposit.getOccurredAt(), deposit.getAmount().amount(),
                requireAccount(accounts, deposit.getAccountId()), deposit.getAtmId()
            );
        }

        if (operation instanceof AtmWithdraw withdraw) {
            return AtmWithdrawEntity.create(
                withdraw.getId(), withdraw.getOccurredAt(), withdraw.getAmount().amount(),
                requireAccount(accounts, withdraw.getAccountId()), withdraw.getAtmId()
            );
        }

        if (operation instanceof Transfer transfer) {
            return TransferEntity.create(
                transfer.getId(), transfer.getOccurredAt(), transfer.getAmount().amount(),
                requireAccount(accounts, transfer.getSourceAccountId()), requireAccount(accounts, transfer.getTargetAccountId())
            );
        }

        if (operation instanceof InterestCapitalization capitalization) {
            AccountEntity account = requireAccount(accounts, capitalization.getAccountId());

            if (!(account instanceof SavingsAccountEntity savings)) {
                throw new IllegalArgumentException("Capitalization requires SavingsAccountEntity");
            }

            return InterestCapitalizationEntity.create(
                capitalization.getId(), capitalization.getOccurredAt(),
                capitalization.getAmount().amount(), savings, capitalization.getMonth()
            );
        }

        throw new IllegalArgumentException("Unsupported operation type: " + operation.getClass());
    }

    public Operation toDomain(OperationEntity entity) {
        Money amount = new Money(entity.getAmount());

        if (entity instanceof AtmDepositEntity deposit) {
            return new AtmDeposit(
                deposit.getId(), deposit.getOccurredAt(), amount,
                deposit.getAccount().getId(), deposit.getAtmId()
            );
        }

        if (entity instanceof AtmWithdrawEntity withdraw) {
            return new AtmWithdraw(
                withdraw.getId(), withdraw.getOccurredAt(), amount,
                withdraw.getAccount().getId(), withdraw.getAtmId()
            );
        }

        if (entity instanceof TransferEntity transfer) {
            return new Transfer(
                transfer.getId(), transfer.getOccurredAt(), amount,
                transfer.getSourceAccount().getId(), transfer.getTargetAccount().getId()
            );
        }

        if (entity instanceof InterestCapitalizationEntity capitalization) {
            return new InterestCapitalization(
                capitalization.getId(), capitalization.getOccurredAt(), amount,
                capitalization.getAccount().getId(), capitalization.getMonth()
            );
        }

        throw new IllegalArgumentException("Unsupported operation entity type: " + entity.getClass());
    }

    private AccountEntity requireAccount(Map<UUID, AccountEntity> accounts, UUID accountId) {
        AccountEntity account = accounts.get(accountId);

        if (account == null) {
            throw new IllegalArgumentException("Account entity not provided: " + accountId);
        }

        return account;
    }
}
