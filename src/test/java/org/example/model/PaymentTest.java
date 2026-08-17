package org.example.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PaymentTest {

    @Test
    void givenPaymentDetails_whenPaymentCreated_thenExposesAllDetails() {
        PaymentType type = new PaymentType("DOMESTIC_FEE");
        NotificationChannel channel = new NotificationChannel("EMAIL");

        Payment payment = new Payment(type, new BigDecimal("4500"), Currency.JOD, channel);

        assertSame(type, payment.getType());
        assertEquals(new BigDecimal("4500"), payment.getAmountInCents());
        assertEquals(Currency.JOD, payment.getCurrency());
        assertSame(channel, payment.getNotificationChannel());
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

        assertEquals(new BigDecimal("150"), payment.getFeeInCents());
        assertEquals(new BigDecimal("2150"), payment.getTotalInCents());
    }
}
