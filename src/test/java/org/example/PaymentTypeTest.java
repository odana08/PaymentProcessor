package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTypeTest {

    @Test
    void givenValidName_whenPaymentTypeCreated_thenExposesName() {
        PaymentType paymentType = new PaymentType("DOMESTIC_FEE");

        assertEquals("DOMESTIC_FEE", paymentType.getName());
        assertEquals("DOMESTIC_FEE", paymentType.toString());
    }

    @Test
    void givenNullName_whenPaymentTypeCreated_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PaymentType(null));

        assertEquals("Payment type cannot be empty", exception.getMessage());
    }

    @Test
    void givenBlankName_whenPaymentTypeCreated_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PaymentType("   "));

        assertEquals("Payment type cannot be empty", exception.getMessage());
    }
}
