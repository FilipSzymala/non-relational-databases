package pl.nbd.bank.bank.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonalIdTest {
    @Test
    void acceptsElevenDigitValue() {
        assertEquals("12345678901", new PersonalId("12345678901").value());
    }

    @Test
    void rejectsNullLettersAndWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new PersonalId(null));
        assertThrows(IllegalArgumentException.class, () -> new PersonalId("123"));
        assertThrows(IllegalArgumentException.class, () -> new PersonalId("1234567890a"));
    }
}
