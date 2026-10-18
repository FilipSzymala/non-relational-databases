package pl.nbd.bank.bank.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Money must not be null");
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Money must not be null");
        return new Money(amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public boolean isLessThan(Money other) {
        Objects.requireNonNull(other, "Money must not be null");
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public static Money pln(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
