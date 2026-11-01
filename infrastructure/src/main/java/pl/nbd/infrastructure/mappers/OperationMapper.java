
package pl.nbd.infrastructure.mappers;

import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.operation.AtmWithdraw;
import pl.nbd.bank.bank.operation.InterestCapitalization;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.bank.operation.Transfer;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.infrastructure.documents.operation.AbstractOperationEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.AtmDepositEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.AtmWithdrawEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.InterestCapitalizationEmbeddedMgd;
import pl.nbd.infrastructure.documents.operation.TransferEmbeddedMgd;

import java.time.YearMonth;
import java.util.Objects;

public class OperationMapper {

    public AbstractOperationEmbeddedMgd toDocument(Operation operation) {
        Objects.requireNonNull(operation, "Operation must not be null");

        if (operation instanceof AtmDeposit deposit) {
            return new AtmDepositEmbeddedMgd(
                deposit.getId(),
                deposit.getOccurredAt(),
                deposit.getAmount().amount(),
                deposit.getAccountId(),
                deposit.getAtmId()
            );
        }

        if (operation instanceof AtmWithdraw withdraw) {
            return new AtmWithdrawEmbeddedMgd(
                withdraw.getId(),
                withdraw.getOccurredAt(),
                withdraw.getAmount().amount(),
                withdraw.getAccountId(),
                withdraw.getAtmId()
            );
        }

        if (operation instanceof InterestCapitalization capitalization) {
            return new InterestCapitalizationEmbeddedMgd(
                capitalization.getId(),
                capitalization.getOccurredAt(),
                capitalization.getAmount().amount(),
                capitalization.getAccountId(),
                capitalization.getMonth().toString()
            );
        }

        if (operation instanceof Transfer transfer) {
            return new TransferEmbeddedMgd(
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

    public Operation toDomain(AbstractOperationEmbeddedMgd document) {
        Objects.requireNonNull(document, "Operation document must not be null");

        if (document instanceof AtmDepositEmbeddedMgd deposit) {
            return new AtmDeposit(
                deposit.getId(),
                deposit.getOccurredAt(),
                new Money(deposit.getAmount()),
                deposit.getAccountId(),
                deposit.getAtmId()
            );
        }

        if (document instanceof AtmWithdrawEmbeddedMgd withdraw) {
            return new AtmWithdraw(
                withdraw.getId(),
                withdraw.getOccurredAt(),
                new Money(withdraw.getAmount()),
                withdraw.getAccountId(),
                withdraw.getAtmId()
            );
        }

        if (document instanceof InterestCapitalizationEmbeddedMgd capitalization) {
            return new InterestCapitalization(
                capitalization.getId(),
                capitalization.getOccurredAt(),
                new Money(capitalization.getAmount()),
                capitalization.getAccountId(),
                YearMonth.parse(capitalization.getMonth())
            );
        }

        if (document instanceof TransferEmbeddedMgd transfer) {
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
