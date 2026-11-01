
package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.operation.AtmWithdraw;
import pl.nbd.bank.bank.operation.InterestCapitalization;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.documents.operation.AbstractOperationEmbedded;
import pl.nbd.infrastructure.documents.operation.AtmDepositEmbedded;
import pl.nbd.infrastructure.documents.operation.AtmWithdrawEmbedded;
import pl.nbd.infrastructure.documents.operation.InterestCapitalizationEmbedded;
import pl.nbd.infrastructure.documents.operation.TransferEmbedded;

import java.time.YearMonth;
import java.util.Objects;

public class OperationMapper {

    public AbstractOperationEmbedded toDocument(Operation operation) {
        Objects.requireNonNull(operation, "Operation must not be null");

        if (operation instanceof AtmDeposit deposit) {
            return new AtmDepositEmbedded(
                deposit.getId(),
                deposit.getOccurredAt(),
                deposit.getAmount().amount(),
                deposit.getAccountId(),
                deposit.getAtmId()
            );
        }

        if (operation instanceof AtmWithdraw withdraw) {
            return new AtmWithdrawEmbedded(
                withdraw.getId(),
                withdraw.getOccurredAt(),
                withdraw.getAmount().amount(),
                withdraw.getAccountId(),
                withdraw.getAtmId()
            );
        }

        if (operation instanceof InterestCapitalization capitalization) {
            return new InterestCapitalizationEmbedded(
                capitalization.getId(),
                capitalization.getOccurredAt(),
                capitalization.getAmount().amount(),
                capitalization.getAccountId(),
                capitalization.getMonth().toString()
            );
        }

        if (operation instanceof Transfer transfer) {
            return new TransferEmbedded(
                transfer.getId(),
                transfer.getOccurredAt(),
                transfer.getAmount().amount(),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId()
            );
        }

        throw new IllegalArgumentException(
            "Unsupported operation type: " + operation.getClass().getName()
        );
    }

    public Operation toDomain(AbstractOperationEmbedded document) {
        Objects.requireNonNull(document, "Operation document must not be null");

        if (document instanceof AtmDepositEmbedded deposit) {
            return new AtmDeposit(
                deposit.getId(),
                deposit.getOccurredAt(),
                new Money(deposit.getAmount()),
                deposit.getAccountId(),
                deposit.getAtmId()
            );
        }

        if (document instanceof AtmWithdrawEmbedded withdraw) {
            return new AtmWithdraw(
                withdraw.getId(),
                withdraw.getOccurredAt(),
                new Money(withdraw.getAmount()),
                withdraw.getAccountId(),
                withdraw.getAtmId()
            );
        }

        if (document instanceof InterestCapitalizationEmbedded capitalization) {
            return new InterestCapitalization(
                capitalization.getId(),
                capitalization.getOccurredAt(),
                new Money(capitalization.getAmount()),
                capitalization.getAccountId(),
                YearMonth.parse(capitalization.getMonth())
            );
        }

        if (document instanceof TransferEmbedded transfer) {
            return new Transfer(
                transfer.getId(),
                transfer.getOccurredAt(),
                new Money(transfer.getAmount()),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId()
            );
        }

        throw new IllegalArgumentException(
            "Unsupported operation document type: " + document.getClass().getName()
        );
    }
}
