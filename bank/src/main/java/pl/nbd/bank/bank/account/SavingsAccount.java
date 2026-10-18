package pl.nbd.bank.bank.account;

import pl.nbd.bank.bank.account.enums.AccountStatus;
import pl.nbd.bank.bank.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public class SavingsAccount extends Account {
    private final BigDecimal annualInterestRate;
    private YearMonth lastInterestCalculationDate;

    public SavingsAccount(UUID id, UUID ownerId, Instant createdAt, AccountNumber accountNumber,
                          Money balance, AccountStatus status, BigDecimal annualInterestRate,
                          YearMonth lastInterestCalculationDate) {
        super(id, ownerId, createdAt, accountNumber, balance, status);
        this.annualInterestRate = Objects.requireNonNull(annualInterestRate, "Interest rate must not be null");
        if (annualInterestRate.signum() < 0 || annualInterestRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Annual interest rate must be between 0 and 1");
        }
        this.lastInterestCalculationDate = lastInterestCalculationDate;
        validateInitialState();
    }

    public SavingsAccount(UUID ownerId, AccountNumber accountNumber, Money balance, AccountStatus status, BigDecimal annualInterestRate, YearMonth lastInterestCalculationDate) {
        super(ownerId, accountNumber, balance, status);
        this.annualInterestRate = Objects.requireNonNull(annualInterestRate, "Interest rate must not be null");
        if (annualInterestRate.signum() < 0 || annualInterestRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Annual interest rate must be between 0 and 1");
        }
        this.lastInterestCalculationDate = lastInterestCalculationDate;
        validateInitialState();
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public YearMonth getLastInterestCalculationDate() {
        return lastInterestCalculationDate;
    }

    @Override
    protected Money minimumAllowedBalance() {
        return Money.ZERO;
    }

    public Money accrueInterest(YearMonth month) {
        Objects.requireNonNull(month, "Month must not be null");
        requireActive();
        if (lastInterestCalculationDate != null && !month.isAfter(lastInterestCalculationDate)) {
            throw new IllegalArgumentException("Interest has already been calculated for this month");
        }
        BigDecimal amount = getBalance().amount().multiply(annualInterestRate).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        Money interest = new Money(amount);
        if (interest.isPositive()) {
            credit(interest);
        }
        lastInterestCalculationDate = month;
        return interest;
    }
}
