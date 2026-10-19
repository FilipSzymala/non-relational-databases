package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.*;
import pl.nbd.infrastructure.entities.client.ClientEntity;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@DiscriminatorValue("SAVINGS")
public class SavingsAccountEntity extends AccountEntity {

    @Column(name = "annual_interest_rate")
    private BigDecimal annualInterestRate;

    @Column(name = "last_interest_calculation_date", length = 7)
    private YearMonth lastInterestCalculationDate;

    protected SavingsAccountEntity() {
        super();
    }

    private SavingsAccountEntity(UUID id, ClientEntity client, BigDecimal annualInterestRate, YearMonth lastInterestCalculationDate) {
        super(id, client);
        this.annualInterestRate = annualInterestRate;
        this.lastInterestCalculationDate = lastInterestCalculationDate;
    }

    public static SavingsAccountEntity create(UUID id, ClientEntity client, BigDecimal annualInterestRate, YearMonth lastInterestCalculationDate) {
        return new SavingsAccountEntity(id, client, annualInterestRate, lastInterestCalculationDate);
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public YearMonth getLastInterestCalculationDate() {
        return lastInterestCalculationDate;
    }
}