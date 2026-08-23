package org.example.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @Test
    void givenPaymentDetails_whenPaymentCreated_thenExposesAllDetails() {
        PaymentType type = new PaymentType("DOMESTIC_FEE");
        NotificationChannel channel = new NotificationChannel("EMAIL");

        Payment payment = new Payment(type, new BigDecimal("4500"), Currency.JOD, channel);

        assertSame(type, payment.getType());
        assertEquals(new BigDecimal("4500.00"), payment.getAmountInCents());
        assertEquals(Currency.JOD, payment.getCurrency());
        assertSame(channel, payment.getNotificationChannels().getFirst());
        assertNotNull(payment.getReference());
        assertEquals(PaymentStatus.CREATED, payment.getStatus());
    }

    @Test
    void givenTwoPayments_whenCreated_thenReferencesAreUnique() {
        PaymentType type = new PaymentType("DOMESTIC_FEE");
        NotificationChannel channel = new NotificationChannel("EMAIL");

        Payment first = new Payment(type, new BigDecimal("4500"), Currency.JOD, channel);
        Payment second = new Payment(type, new BigDecimal("4500"), Currency.JOD, channel);

        assertNotEquals(first.getReference(), second.getReference());
    }

    @Test
    void givenCreatedPayment_whenMarkedAsProcessed_thenStatusIsProcessed() {
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("4500"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );

        payment.markAsProcessed();

        assertEquals(PaymentStatus.PROCESSED, payment.getStatus());
    }

    @Test
    void givenPaymentFee_whenFeeSet_thenExposesFeeAndTotal() {
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("2000"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );

        payment.setFeeInCents(new BigDecimal("150"));

        assertEquals(new BigDecimal("150.00"), payment.getFeeInCents());
        assertEquals(new BigDecimal("2150.00"), payment.getTotalInCents());
    }

    @Test
    void givenNonPositiveAmount_whenPaymentCreated_thenRejectsIt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        new PaymentType("DOMESTIC_FEE"),
                        BigDecimal.ZERO,
                        Currency.JOD,
                        new NotificationChannel("EMAIL")
                )
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void givenAmountWithMoreThanTwoDecimalPlaces_whenPaymentCreated_thenRejectsIt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        new PaymentType("INTERNATIONAL_FEE"),
                        new BigDecimal("1.001"),
                        Currency.JOD,
                        new NotificationChannel("EMAIL")
                )
        );

        assertEquals("Amount cannot have more than two decimal places", exception.getMessage());
    }

    @Test
    void givenNoNotificationChannels_whenPaymentCreated_thenRejectsIt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        new PaymentType("DOMESTIC_FEE"),
                        BigDecimal.ONE,
                        Currency.JOD,
                        List.of()
                )
        );

        assertEquals("At least one notification channel is required", exception.getMessage());
    }

    @Test
    void givenNegativeFee_whenAssigned_thenRejectsIt() {
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                BigDecimal.ONE,
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payment.setFeeInCents(new BigDecimal("-0.01"))
        );

        assertEquals("Fee cannot be negative", exception.getMessage());
    }

    @Test
    void givenSubCentFee_whenAssigned_thenRoundsHalfUpToDatabaseScale() {
        Payment payment = new Payment(
                new PaymentType("INTERNATIONAL_FEE"),
                new BigDecimal("50.25"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );

        payment.setFeeInCents(new BigDecimal("1.005"));

        assertEquals(new BigDecimal("1.01"), payment.getFeeInCents());
    }

}
