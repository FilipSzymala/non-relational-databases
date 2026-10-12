package pl.nbd.model.Account;

import pl.nbd.model.Account.enums.AccountStatus;
import pl.nbd.model.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public class SavingsAccount extends Account {
    private BigDecimal annualInterestRate;
    private YearMonth lastInterestCalculationDate;

    public SavingsAccount(UUID ownerId, AccountNumber accountNumber, Money balance, AccountStatus status, BigDecimal annualInterestRate, YearMonth lastInterestCalculationDate) {
        super(ownerId, accountNumber, balance, status);
        this.annualInterestRate = annualInterestRate;
        this.lastInterestCalculationDate = lastInterestCalculationDate;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public YearMonth getLastInterestCalculationDate() {
        return lastInterestCalculationDate;
    }

}
