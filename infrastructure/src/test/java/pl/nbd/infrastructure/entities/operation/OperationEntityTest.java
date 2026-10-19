package pl.nbd.infrastructure.entities.operation;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class OperationEntityTest {

    @Test
    void shouldHaveAbstractBaseEntity() {
        assertTrue(Modifier.isAbstract(OperationEntity.class.getModifiers()));
        assertNotNull(OperationEntity.class.getAnnotation(Entity.class));
    }

    @Test
    void shouldUseSingleTableInheritanceWithOperationType() {
        Inheritance inheritance = OperationEntity.class.getAnnotation(Inheritance.class);
        assertNotNull(inheritance);
        assertEquals(InheritanceType.SINGLE_TABLE, inheritance.strategy());

        DiscriminatorColumn discriminator = OperationEntity.class.getAnnotation(DiscriminatorColumn.class);
        assertNotNull(discriminator);
        assertEquals("operation_type", discriminator.name());
    }

    @Test
    void shouldMapAllFourConcreteSubtypes() {
        assertSubtype(AtmDepositEntity.class, "ATM_DEPOSIT");
        assertSubtype(AtmWithdrawEntity.class, "ATM_WITHDRAW");
        assertSubtype(TransferEntity.class, "TRANSFER");
        assertSubtype(InterestCapitalizationEntity.class, "INTEREST_CAPITALIZATION");
    }

    private static void assertSubtype(Class<? extends OperationEntity> subtype, String expected) {
        assertNotNull(subtype.getAnnotation(Entity.class));
        assertEquals(OperationEntity.class, subtype.getSuperclass());
        DiscriminatorValue discriminator = subtype.getAnnotation(DiscriminatorValue.class);
        assertNotNull(discriminator);
        assertEquals(expected, discriminator.value());
    }
}
