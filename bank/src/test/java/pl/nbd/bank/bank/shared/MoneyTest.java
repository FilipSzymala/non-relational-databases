package pl.nbd.bank.bank.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void normalizesMoneyToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.00"), Money.pln("12").amount());
        assertEquals(new BigDecimal("12.50"), Money.pln("12.5").amount());
    }

    @Test
    void addsAndSubtractsMoney() {
        assertEquals(Money.pln("12.50"), Money.pln("10.00").add(Money.pln("2.50")));
        assertEquals(Money.pln("7.50"), Money.pln("10.00").subtract(Money.pln("2.50")));
    }

    @Test
    void negatesMoney() {
        assertEquals(Money.pln("-15.00"), Money.pln("15.00").negate());
    }

    @Test
    void comparesAndRecognizesZeroAndPositiveAmounts() {
        assertTrue(Money.pln("-1.00").isLessThan(Money.ZERO));
        assertTrue(Money.ZERO.isZero());
        assertTrue(Money.pln("0.01").isPositive());
        assertFalse(Money.pln("-0.01").isPositive());
    }

    @Test
    void rejectsFractionalCents() {
        assertThrows(ArithmeticException.class, () -> Money.pln("1.001"));
    }

    @Test
    void rejectsNullAmountAndNullArithmeticOperand() {
        assertThrows(NullPointerException.class, () -> new Money(null));
        assertThrows(NullPointerException.class, () -> Money.ZERO.add(null));
        assertThrows(NullPointerException.class, () -> Money.ZERO.subtract(null));
    }
}
