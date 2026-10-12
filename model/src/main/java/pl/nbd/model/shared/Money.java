package pl.nbd.model.shared;

import java.math.BigDecimal;


public record Money(BigDecimal amount, Currency currency) {
}
